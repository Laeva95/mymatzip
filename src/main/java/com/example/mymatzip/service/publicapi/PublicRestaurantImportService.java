package com.example.mymatzip.service.publicapi;

import com.example.mymatzip.dto.publicapi.ImportReport;
import com.example.mymatzip.dto.publicapi.PublicRestaurantRow;
import com.example.mymatzip.dto.publicapi.PublicRestaurantRow.Outcome;
import com.example.mymatzip.entity.Member;
import com.example.mymatzip.entity.Restaurant;
import com.example.mymatzip.entity.Source;
import com.example.mymatzip.repository.RestaurantRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;   // [추가]2026.11.11권성애 (v3)
import java.util.LinkedHashMap;   // [추가]2026.11.11권성애
import java.util.List;
import java.util.Map;

// ============================================================
// [PublicRestaurantImportService] 공공 API 맛집 조사(preview) · 저장(import)
// ============================================================
// preview : DB에 저장하지 않고 "저장하면 어떻게 되는지"만 보여 줌
//           - 모범음식점 bsnsCond(업태) 값별 건수 → PublicCategoryMapper 매핑표 확정용
//           - 건마다 분류 결과([분류] 이름 → 카테고리 (근거))
//           - 중복 · 제외 · 실패 예상, 데이터 점검(좌표 없음 · 전화 없음 · 이미지 없음 ...)
// import  : 한 건씩 중복 검사 후 저장. 한 건이 실패해도 나머지는 계속 저장.
//
// [추가]2026.11.11권성애 (v3 수정) 같은 가게 판단 (preview · import 공통)
//   같은 가게 = 비교용 주소(comparingAddress)가 같고 + 이름이 같거나 한쪽이 다른 쪽을 포함(isSameStoreName)
//   1) 같은 API 안: 같은 가게가 여러 번이면 1건만 남김 (keepLatest)
//        - 내용(주소 · 메뉴 등)은 최신 기록 (모범음식점 dataDay 큰 것, 같거나 없으면 나중에 나온 것)
//        - 이름은 둘 중 긴 이름 (selectLongerName — "본점" · "OO점" 같은 지점 표시가 보이게)
//   2) DB와 비교: 시작할 때 한 번 DB 맛집(소프트 삭제된 것 포함)을 비교용 주소별로 모아서(getStoresByAddressAtStartOnly) 비교
//        → 모범음식점과 부산맛집 양쪽에 있는 가게, 이미 넣은 가게, 삭제한 가게가 다시 들어가지 않음
//        → DB 이름이 더 짧으면 이름은 바꾸지 않고 [확인필요]에 바꾸기 전 · 후 이름만 남김 (관리자가 고친 이름을 덮어쓰지 않게)
//   3) [확인필요] (저장은 함, 관리자 확인용 — 콘솔 + CSV)
//        - 좌표가 NEARBY_METERS(20m) 안인데 이름이 다른 가게 (같은 가게인데 이름이 아예 다르거나, 주소 오타 · 지번 주소일 때)
//        - 이름은 같은데 주소가 다른 가게 (지점이거나 이전한 가게)
//   DB 유니크 키(이름+주소)는 글자가 완전히 같을 때만 막으므로 위 검사가 먼저, 유니크 키는 마지막 안전장치
//
// 기록: 콘솔(log) + CSV 파일(import-results/ 폴더) + API 응답(ImportReport)
//   형식: [구분] 출처 | 업체명 | 주소 | 이유
//   성공은 한 줄씩 남기지 않고 요약 숫자만 (중요한 중복 · 실패가 묻히지 않게)
//
// 트랜잭션: 이 메서드에는 @Transactional을 붙이지 않습니다.
//   restaurantRepository.save()가 건마다 자기 트랜잭션으로 커밋 → 한 건 실패가 전체를 되돌리지 않음.
//   (saveAll()을 한 트랜잭션으로 묶으면 중복 한 건에 전체가 롤백됨)
// ============================================================
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicRestaurantImportService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Path RESULT_DIR = Paths.get("import-results");   // .gitignore에 추가하세요
    // [추가]2026.11.11권성애 (v3) [확인필요] 좌표 거리 기준: 20m (이 안에 이름이 다른 가게가 있으면 기록)
    private static final double NEARBY_METERS = 20;

    // [추가]2026.11.11권성애 (v3) 비교에 쓰는 DB 맛집 한 건 (id는 preview에서 이번에 저장 예정인 것이면 null)
    // [변경]2026.11.12권성애 (address 추가: [확인필요] · [중복] 문구에 상대 가게 주소를 보여 주려고)
    private record DbStore(Long id, String name, Source source, Double lat, Double lng, String address) {}


    private final PublicApiClient client;
    private final PublicRestaurantMapper mapper;
    private final RestaurantRepository restaurantRepository;
    private final EntityManager entityManager;

    /** 조사 모드: 저장하지 않음 */
    public ImportReport preview(Source source, int limit) {
        ImportReport report = new ImportReport(source.getDisplayName(), "preview");
        List<PublicRestaurantRow> rows = fetchAndMap(source, limit, report);

        rows = keepLatest(rows, report);           // [추가]2026.11.11권성애 같은 API 안 같은 가게 → 1건만 (최신 기록 + 긴 이름)
        Map<String, List<DbStore>> stores = getStoresByAddressAtStartOnly();   // [추가]2026.11.11권성애 (v3) DB 맛집을 비교용 주소별로
        List<DbStore> allStores = flatten(stores);                            // [추가]2026.11.11권성애 (v3) 좌표 · 이름 비교용 전체 목록
        for (PublicRestaurantRow row : rows) {
            // 분류 결과는 preview에서만 한 줄씩 보여 줌 (키워드표 점검용)
            report.addLine(line("분류", row, row.category().getDisplayName() + " (" + row.categoryReason() + ")"));

            if (row.outcome() == Outcome.FAIL) {
                fail(report, row, row.reason());
            } else if (row.outcome() == Outcome.EXCLUDE) {
                exclude(report, row);
            } else if (skipIfSameStoreInDb(report, row, stores)) {   // [추가]2026.11.11권성애 (v3 수정: 같은 주소 + 같은 가게 이름)
                // 중복 · [확인필요](이름 길이) 기록은 위 메서드 안에서
            } else {
                checkSameNameOtherAddress(report, row, allStores);   // [추가]2026.11.11권성애 (v3) [확인필요]
                checkOtherNameWithinMeters(report, row, allStores);  // [추가]2026.11.11권성애 (v3) [확인필요]
                report.addSuccess();                       // 저장 예정
                if (row.unmapped()) unmapped(report, row);
                addStore(stores, allStores, new DbStore(null, row.name(), row.source(), row.lat(), row.lng(), row.address()), row.address());   // [변경]2026.11.12권성애 (주소 추가)
            }
        }
        finish(report, source);
        return report;
    }

    /** 저장 모드: 관리자를 등록자로 저장 */
    public ImportReport importAll(Source source, int limit, Member admin) {
        if (admin == null) {
            throw new IllegalArgumentException("로그인한 관리자 정보가 없습니다.");
        }
        ImportReport report = new ImportReport(source.getDisplayName(), "import");
        List<PublicRestaurantRow> rows = fetchAndMap(source, limit, report);

        rows = keepLatest(rows, report);           // [추가]2026.11.11권성애 같은 API 안 같은 가게 → 1건만 (최신 기록 + 긴 이름)
        Map<String, List<DbStore>> stores = getStoresByAddressAtStartOnly();   // [추가]2026.11.11권성애 (v3) DB 맛집을 비교용 주소별로
        List<DbStore> allStores = flatten(stores);                            // [추가]2026.11.11권성애 (v3) 좌표 · 이름 비교용 전체 목록

        int saved = 0;
        for (PublicRestaurantRow row : rows) {
            if (row.outcome() == Outcome.FAIL) {
                fail(report, row, row.reason());
                continue;
            }
            if (row.outcome() == Outcome.EXCLUDE) {
                exclude(report, row);
                continue;
            }
            // 저장 전 중복 검사 (유니크 제약은 최종 안전장치)
            // [추가]2026.11.11권성애 (v3 수정: 같은 주소 + 같은 가게 이름이면 건너뜀, 표기 · 지점 표시만 다른 같은 가게 포함)
            if (skipIfSameStoreInDb(report, row, stores)) {
                continue;
            }
            checkSameNameOtherAddress(report, row, allStores);   // [추가]2026.11.11권성애 (v3) [확인필요]
            checkOtherNameWithinMeters(report, row, allStores);  // [추가]2026.11.11권성애 (v3) [확인필요]
            try {
                Restaurant savedEntity = restaurantRepository.save(toEntity(row, admin));
                report.addSuccess();
                if (row.unmapped()) unmapped(report, row);
                addStore(stores, allStores, new DbStore(savedEntity.getId(), row.name(), row.source(), row.lat(), row.lng(), row.address()), row.address());   // [변경]2026.11.12권성애 (주소 추가)
                saved++;
                if (saved % 100 == 0) {
                    entityManager.clear();   // 저장한 엔티티가 영속성 컨텍스트에 계속 쌓이지 않게
                }
            } catch (DataIntegrityViolationException e) {
                // 검사와 저장 사이에 다른 요청이 같은 맛집을 넣은 경우 등
                entityManager.clear();       // 실패한 엔티티를 비워야 다음 저장이 정상 동작
                duplicate(report, row, "유니크 제약 위반(이름+주소)");
            } catch (RuntimeException e) {
                entityManager.clear();
                fail(report, row, "DB 오류: " + e.getMessage());
            }
        }
        finish(report, source);
        return report;
    }

    // ── API 호출 + 정리 ─────────────────────────────────────────
    private List<PublicRestaurantRow> fetchAndMap(Source source, int limit, ImportReport report) {
        List<Map<String, Object>> items = client.fetchAll(source, limit);
        report.setFetched(items.size());

        List<PublicRestaurantRow> rows = new ArrayList<>();
        for (Map<String, Object> item : items) {
            PublicRestaurantRow row = (source == Source.PUBLIC_API_BUSAN_FOOD)
                    ? mapper.mapFood(item)
                    : mapper.mapModel(item);
            rows.add(row);

            // 데이터 점검 (preview · import 공통)
            if (source == Source.PUBLIC_API_BUSAN_MODEL) report.countRawCategory(row.rawCategory());
            report.countCategory(row.category().getDisplayName());
            if (row.lat() == null || row.lng() == null) report.check("좌표 없음");
            if (row.phone() == null) report.check("전화번호 없음");
            if (row.imageUrl() == null) report.check("이미지 없음");
            if (row.representativeMenu() == null) report.check("대표메뉴 없음");
            if (row.description() != null && row.description().length() > 500) report.check("소개글 500자 초과(그대로 저장)");
            if (row.businessHours() != null && row.businessHours().contains("휴업")) report.check("휴업 표기 있음");
            if (row.name() != null && row.name().length() > 50) report.check("이름 50자 초과(폼 제한보다 김)");
            if (row.address() != null && !mapper.hasGugun(row.address())) report.check("구 없음");   // [추가]2026.11.11권성애 구군 값도 없어 못 붙인 주소
        }
        return rows;
    }

    // [추가]2026.11.11권성애 (v3 수정) 같은 API 안에서 같은 가게(비교용 주소 같음 + isSameStoreName)는 1건만 남기고 나머지는 [중복]
    //   남기는 행: 내용은 최신 기록 (dataDate "2025-12-05" 형식이 큰 것, 같거나 없으면 나중에 나온 것)
    //              이름은 둘 중 긴 이름 (selectLongerName)
    //   FAIL · EXCLUDE 행은 그대로 두어 원래대로 [실패] · [제외] 기록
    private List<PublicRestaurantRow> keepLatest(List<PublicRestaurantRow> rows, ImportReport report) {
        Map<String, List<PublicRestaurantRow>> byAddress = new LinkedHashMap<>();   // 비교용 주소 → 남긴 행들
        List<PublicRestaurantRow> result = new ArrayList<>();

        for (PublicRestaurantRow row : rows) {
            if (row.outcome() != Outcome.READY) {
                result.add(row);
                continue;
            }
            List<PublicRestaurantRow> kept = byAddress.computeIfAbsent(
                    PublicRestaurantRow.comparingAddress(row.address()), k -> new ArrayList<>());
            int same = -1;
            for (int i = 0; i < kept.size(); i++) {
                if (PublicRestaurantRow.isSameStoreName(kept.get(i).name(), row.name())) {
                    same = i;
                    break;
                }
            }
            if (same < 0) {
                kept.add(row);
                continue;
            }
            PublicRestaurantRow before = kept.get(same);
            boolean rowIsNewer = compareDate(row.dataDate(), before.dataDate()) >= 0;
            PublicRestaurantRow newer = rowIsNewer ? row : before;
            PublicRestaurantRow older = rowIsNewer ? before : row;
            String name = selectLongerName(newer.name(), older.name());
            PublicRestaurantRow merged = new PublicRestaurantRow(newer.source(), name, newer.address(), newer.phone(),
                    newer.description(), newer.representativeMenu(), newer.businessHours(), newer.imageUrl(),
                    newer.lat(), newer.lng(), newer.rawCategory(), newer.category(), newer.categoryReason(),
                    newer.unmapped(), newer.outcome(), newer.reason(), newer.dataDate());
            kept.set(same, merged);
            duplicate(report, older, "같은 API 안에서 중복 → 최신 기록만 남김 (남긴 이름: " + name
                    + ", 남긴 주소: " + newer.address()
                    + (newer.dataDate() != null ? ", 기준일 " + newer.dataDate() : "")
                    + (older.dataDate() != null ? " / 버린 기록 기준일 " + older.dataDate() : "") + ")");
        }
        for (List<PublicRestaurantRow> kept : byAddress.values()) {
            result.addAll(kept);
        }
        return result;
    }

    // null은 가장 오래된 것으로
    private int compareDate(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        return a.compareTo(b);
    }

    // [추가]2026.11.11권성애 (v3) 두 이름 중 긴 이름 (같으면 첫 번째) — "이재모피자" / "이재모피자 본점" → "이재모피자 본점"
    private String selectLongerName(String a, String b) {
        if (a == null) return b;
        if (b == null) return a;
        return (b.replaceAll("\\s+", "").length() > a.replaceAll("\\s+", "").length()) ? b : a;
    }

    // [추가]2026.11.11권성애 (v3, loadDbKeys 대신) 시작할 때 한 번만 DB 맛집(소프트 삭제 포함)을 읽어 "비교용 주소 → 그 주소의 가게들"로 모음
    //   findAll()은 deletedAt 조건이 없어 삭제된 맛집도 포함됨 (삭제한 가게가 배치로 되살아나지 않게)
    //   맛집이 몇천 건 수준이라 한 번 읽어도 부담 없음
    private Map<String, List<DbStore>> getStoresByAddressAtStartOnly() {
        Map<String, List<DbStore>> stores = new HashMap<>();
        for (Restaurant r : restaurantRepository.findAll()) {
            stores.computeIfAbsent(PublicRestaurantRow.comparingAddress(r.getAddress()), k -> new ArrayList<>())
                    .add(new DbStore(r.getId(), r.getName(), r.getSource(), r.getLat(), r.getLng(), r.getAddress()));   // [변경]2026.11.12권성애 (주소 추가)
        }
        entityManager.clear();   // 읽은 엔티티를 영속성 컨텍스트에서 비움
        return stores;
    }

    // 주소별 목록 → 전체 목록 (좌표 · 이름 비교용)
    private List<DbStore> flatten(Map<String, List<DbStore>> stores) {
        List<DbStore> all = new ArrayList<>();
        for (List<DbStore> list : stores.values()) {
            all.addAll(list);
        }
        return all;
    }

    // 이번에 저장(예정)한 가게도 다음 행과 비교되게 추가
    private void addStore(Map<String, List<DbStore>> stores, List<DbStore> allStores, DbStore store, String address) {
        stores.computeIfAbsent(PublicRestaurantRow.comparingAddress(address), k -> new ArrayList<>()).add(store);
        allStores.add(store);
    }

    // [추가]2026.11.11권성애 (v3) 같은 비교용 주소에 같은 가게 이름이 있으면 그 가게, 없으면 null
    private DbStore getStoreFromComparingAddress(PublicRestaurantRow row, Map<String, List<DbStore>> stores) {
        List<DbStore> list = stores.get(PublicRestaurantRow.comparingAddress(row.address()));
        if (list == null) return null;
        for (DbStore s : list) {
            if (PublicRestaurantRow.isSameStoreName(s.name(), row.name())) {
                return s;
            }
        }
        return null;
    }

    // DB(또는 이번 실행에서 먼저 저장한 것)에 같은 가게가 있으면 [중복] 기록 후 true
    //   DB 이름이 더 짧으면 이름은 바꾸지 않고 [확인필요]에 바꾸기 전 · 후 이름을 남김
    private boolean skipIfSameStoreInDb(ImportReport report, PublicRestaurantRow row, Map<String, List<DbStore>> stores) {
        DbStore same = getStoreFromComparingAddress(row, stores);
        if (same == null) {
            return false;
        }
        duplicate(report, row, "이미 등록된 맛집과 같음: \"" + same.name() + "\"" + idText(same)
                + " (주소 표기 · 지점 표시 차이 포함, 삭제된 맛집 포함)");
        if (selectLongerName(same.name(), row.name()).equals(row.name()) && !same.name().equals(row.name())) {
            confirm(report, row, "DB에 같은 가게 \"" + same.name() + "\"" + idText(same)
                    + " — 더 긴 이름 \"" + row.name() + "\"(으)로 바꿀지 확인 (자동으로 바꾸지 않음)");
        }
        return true;
    }

    // [추가]2026.11.11권성애 (v3) 이름은 같은데 비교용 주소가 다른 가게 → [확인필요] (지점이면 그대로, 이전한 가게면 옛 것을 관리자가 삭제)
    private void checkSameNameOtherAddress(ImportReport report, PublicRestaurantRow row, List<DbStore> allStores) {
        for (DbStore s : allStores) {
            // 같은 주소의 같은 가게는 앞(skipIfSameStoreInDb)에서 이미 걸렀으므로, 여기서 걸리는 것은 주소가 다른 곳
            if (PublicRestaurantRow.isSameStoreName(s.name(), row.name())) {
                confirm(report, row, "이름이 같은 가게가 다른 주소에 있음: \"" + s.name() + "\"" + idText(s)
                        + " — 지점인지 이전한 가게인지 확인");
                return;   // 한 줄만
            }
        }
    }

    // [추가]2026.11.11권성애 (v3) 좌표가 NEARBY_METERS(20m) 안인데 이름이 다른 가게 → [확인필요] (같은 가게인데 이름이 아예 다르거나, 같은 건물의 다른 가게)
    private void checkOtherNameWithinMeters(ImportReport report, PublicRestaurantRow row, List<DbStore> allStores) {
        if (row.lat() == null || row.lng() == null) return;
        for (DbStore s : allStores) {
            if (s.lat() == null || s.lng() == null || PublicRestaurantRow.isSameStoreName(s.name(), row.name())) {
                continue;
            }
            double d = distanceMeter(row.lat(), row.lng(), s.lat(), s.lng());
            if (d <= NEARBY_METERS) {
                confirm(report, row, "좌표 " + Math.round(d) + "m 안에 이름이 다른 가게: \"" + s.name() + "\"" + idText(s)
                        + " — 같은 가게인지 확인");
                return;   // 가장 먼저 찾은 한 곳만
            }
        }
    }

    // [추가]2026.11.11권성애 (v3) 두 좌표 사이 거리(m) — 하버사인 공식 (지구 반지름 6,371km)
    private double distanceMeter(double lat1, double lng1, double lat2, double lng2) {
        double r = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }

    // "(id 12)" — preview에서 이번 저장 예정분(id 없음)이면 "(이번 실행)"
// "(id 12, 부산광역시 …)" — preview에서 이번 저장 예정분(id 없음)이면 "(이번 실행, 부산광역시 …)"
// [변경]2026.11.12권성애 (상대 가게 주소도 함께 표시)
    private String idText(DbStore s) {
        String where = (s.id() != null) ? "id " + s.id() : "이번 실행";
        return "(" + where + ", " + s.address() + ")";
    }


    private Restaurant toEntity(PublicRestaurantRow row, Member admin) {
        return Restaurant.builder()
                .name(row.name())
                .category(row.category())
                .address(row.address())
                .phone(row.phone())
                .description(row.description())
                .imageUrl(row.imageUrl())
                .representativeMenu(row.representativeMenu())
                .businessHours(row.businessHours())
                .lat(row.lat())
                .lng(row.lng())
                .source(row.source())
                .member(admin)
                .build();
    }

    // ── 기록 ─────────────────────────────────────────────────────
    private void duplicate(ImportReport report, PublicRestaurantRow row, String reason) {
        report.addDuplicate();
        record(report, line("중복", row, reason));
    }

    private void exclude(ImportReport report, PublicRestaurantRow row) {
        report.addExcluded();
        record(report, line("제외", row, row.reason()));
    }

    private void fail(ImportReport report, PublicRestaurantRow row, String reason) {
        report.addFailed();
        record(report, line("실패", row, reason));
    }

    // [추가]2026.11.11권성애 (v3) [확인필요] — 저장은 그대로, 관리자 확인용 기록 (콘솔 + CSV + 응답)
    private void confirm(ImportReport report, PublicRestaurantRow row, String reason) {
        report.addConfirmNeeded();
        record(report, line("확인필요", row, reason));
    }

    private void unmapped(ImportReport report, PublicRestaurantRow row) {
        report.addUnmapped();
        record(report, line("매핑없음", row,
                (row.rawCategory() != null ? "bsnsCond=\"" + row.rawCategory() + "\" " : "") + "→ " + row.category().getDisplayName() + "로 저장 (" + row.categoryReason() + ")"));
    }

    private void record(ImportReport report, String line) {
        report.addLine(line);
        log.info(line);
    }

    // [구분] 출처 | 업체명 | 주소 | 이유
    private String line(String kind, PublicRestaurantRow row, String reason) {
        return "[" + kind + "] " + row.source().getDisplayName()
                + " | " + (row.name() == null ? "(이름 없음)" : row.name())
                + " | " + (row.address() == null ? "(주소 없음)" : row.address())
                + " | " + reason;
    }

    // ── 마무리: 요약 출력 + CSV 저장 ────────────────────────────
    private void finish(ImportReport report, Source source) {
        log.info("──────────────────────────────");
        log.info(report.summary());
        if (!report.getRawCategoryCounts().isEmpty()) {
            log.info("[bsnsCond 값별 건수] {}", report.getRawCategoryCounts());
        }
        log.info("[카테고리별 건수] {}", report.getCategoryCounts());
        log.info("[데이터 점검] {}", report.getChecks());
        report.setResultFile(writeCsv(report, source));
    }

    // 엑셀에서 한글이 깨지지 않게 UTF-8 BOM을 붙여 저장
    private String writeCsv(ImportReport report, Source source) {
        String time = LocalDateTime.now(SEOUL).format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path file = RESULT_DIR.resolve("import-result-" + source.name().toLowerCase() + "-" + report.getMode() + "-" + time + ".csv");

        List<String> csv = new ArrayList<>();
        csv.add("구분,출처,업체명,주소,이유");
        for (String l : report.getLines()) {
            // "[중복] 출처 | 업체명 | 주소 | 이유" → 칸 5개
            int close = l.indexOf(']');
            String kind = l.substring(1, close);
            String[] parts = l.substring(close + 2).split(" \\| ", 4);
            StringBuilder sb = new StringBuilder(cell(kind));
            for (int i = 0; i < 4; i++) {
                sb.append(',').append(cell(i < parts.length ? parts[i] : ""));
            }
            csv.add(sb.toString());
        }
        csv.add("");
        csv.add(cell(report.summary()));
        csv.add(cell("bsnsCond 값별 건수: " + report.getRawCategoryCounts()));
        csv.add(cell("카테고리별 건수: " + report.getCategoryCounts()));
        csv.add(cell("데이터 점검: " + report.getChecks()));

        try {
            Files.createDirectories(RESULT_DIR);
            Files.writeString(file, "﻿" + String.join("\r\n", csv), StandardCharsets.UTF_8);
            log.info("[결과 파일] {}", file.toAbsolutePath());
            return file.toAbsolutePath().toString();
        } catch (IOException e) {
            log.warn("[결과 파일] 저장 실패: {}", e.getMessage());
            return "저장 실패: " + e.getMessage();
        }
    }

    private String cell(String value) {
        return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\"";
    }
}
