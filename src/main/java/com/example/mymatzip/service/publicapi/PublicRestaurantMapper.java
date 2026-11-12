package com.example.mymatzip.service.publicapi;

import com.example.mymatzip.dto.publicapi.PublicRestaurantRow;
import com.example.mymatzip.dto.publicapi.PublicRestaurantRow.Outcome;
import com.example.mymatzip.entity.Source;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.regex.Pattern;

// ============================================================
// [PublicRestaurantMapper] 공공 API item(Map) 1건 → PublicRestaurantRow
// ============================================================
// 정리 규칙 (설계 PDF 3장)
//   1. 이름 · 주소 · 전화: 탭 · 줄바꿈 → 공백 1칸, 앞뒤 공백 제거 (trim)
//   2. 주소 통일 (순서 중요)
//        "부산광역시 "로 시작 → 그대로
//        "부산 "(공백 포함)으로 시작 → "부산광역시 "로 바꿈   ※ "부산진구"는 공백이 없어 해당 안 됨
//        둘 다 아니면 → 앞에 "부산광역시 " 붙임
//      // [추가]2026.11.11권성애 추가 단계 (preview CSV에서 발견)
//        2-1. 끝의 ", Busan, 47294, 대한민국" / ", Busan, 48285, 한국" 꼬리 지움 (부산맛집 약 60건)
//        2-2. "부산시 "로 시작하거나 "부산광역시 부산시 "로 된 것도 "부산광역시 "로 (예: 삼진어묵 본점)
//        2-3. "부산광역시 " 다음에 구군이 없으면 API의 구군 값을 끼워 넣음
//             예) "부산광역시 남천동로108번길 43" + 구군 "수영구" → "부산광역시 수영구 남천동로108번길 43"
//             구군은 정규식이 아니라 16개 목록과 비교 (구덕로 · 구남로 · 남천동로를 구로 잘못 보지 않게)
//   3. 휴업: 영업시간에 "휴업"이 있고 올해 연도(2026 / 26.)가 없으면 제외
//   4. 이미지: 전체 URL이면 그대로, "/uploadImgs/..."처럼 경로만 오면 비짓부산 주소를 붙임
//   5. 좌표: 숫자 · 문자열 모두 Double로. 없거나 0이면 실패
//   6. 필수값: 이름 · 주소 없으면 실패
// ============================================================
@Component
@RequiredArgsConstructor
public class PublicRestaurantMapper {

    private static final String VISITBUSAN = "https://www.visitbusan.net";
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    // [추가]2026.11.11권성애 부산 16개 구군
    private static final java.util.List<String> GUGUN = java.util.List.of(
            "중구", "서구", "동구", "영도구", "부산진구", "동래구", "남구", "북구",
            "해운대구", "사하구", "금정구", "강서구", "연제구", "수영구", "사상구", "기장군");
    // [추가]2026.11.11권성애 주소 끝 영문 꼬리: ", Busan, 47294, 대한민국" (우편번호 · 나라 이름은 없을 수도 있음)
    private static final Pattern ENGLISH_TAIL =
            Pattern.compile("\\s*,\\s*Busan(\\s*,\\s*\\d{5})?(\\s*,\\s*(대한민국|한국|Korea|South Korea))?\\s*$",
                    Pattern.CASE_INSENSITIVE);

    private final PublicCategoryMapper categoryMapper;

    /** 부산맛집정보 서비스 (getFoodKr) */
    public PublicRestaurantRow mapFood(Map<String, Object> item) {
        String name = oneLine(item.get("MAIN_TITLE"));
        String address = normalizeAddress(oneLine(item.get("ADDR1")), oneLine(item.get("GUGUN_NM")));   // [추가]2026.11.11권성애 (수정: 구군 전달)
        String phone = oneLine(item.get("CNTCT_TEL"));
        String description = multiLine(item.get("ITEMCNTNTS"));
        String menu = multiLine(item.get("RPRSNTV_MENU"));
        String hours = multiLine(item.get("USAGE_DAY_WEEK_AND_TIME"));
        String image = imageUrl(oneLine(item.get("MAIN_IMG_NORMAL")));
        Double lat = toDouble(item.get("LAT"));
        Double lng = toDouble(item.get("LNG"));

        PublicCategoryMapper.Result cat = categoryMapper.mapFood(menu, oneLine(item.get("TITLE")), name, description);

        return decide(Source.PUBLIC_API_BUSAN_FOOD, name, address, phone, description, menu, hours, image,
                lat, lng, null, cat, null);   // [추가]2026.11.11권성애 (수정: 기준일 없음 → null)
    }

    /** 구군 모범음식점 현황 (getTblFnrstrnStusInfo) */
    public PublicRestaurantRow mapModel(Map<String, Object> item) {
        String name = oneLine(item.get("bsnsNm"));
        // [추가]2026.11.11권성애 (수정: 도로명 주소가 없으면 지번 주소, 구군 전달)
        String road = oneLine(item.get("addrRoad"));
        String address = normalizeAddress(road != null ? road : oneLine(item.get("addrJibun")), oneLine(item.get("gugun")));
        String phone = oneLine(item.get("tel"));
        String menu = oneLine(item.get("menu"));
        String bsnsCond = oneLine(item.get("bsnsCond"));
        Double lat = toDouble(item.get("lat"));
        Double lng = toDouble(item.get("lng"));

        PublicCategoryMapper.Result cat = categoryMapper.mapModel(bsnsCond, menu, name);

        // [추가]2026.11.11권성애 데이터 기준일 (같은 가게가 여러 번 나오면 최신 기록을 남기는 기준), 없으면 ovrdDate
        String dataDate = oneLine(item.get("dataDay"));
        if (dataDate == null) dataDate = oneLine(item.get("ovrdDate"));

        // 모범음식점은 소개글 · 영업시간 · 이미지가 없음
        return decide(Source.PUBLIC_API_BUSAN_MODEL, name, address, phone, null, menu, null, null,
                lat, lng, bsnsCond, cat, dataDate);   // [추가]2026.11.11권성애 (수정: 기준일 전달)
    }

    // ── 저장 대상 / 제외 / 실패 판정 ───────────────────────────
    private PublicRestaurantRow decide(Source source, String name, String address, String phone,
                                       String description, String menu, String hours, String image,
                                       Double lat, Double lng, String rawCategory,
                                       PublicCategoryMapper.Result cat,
                                       String dataDate) {   // [추가]2026.11.11권성애 (수정: dataDate 추가)
        Outcome outcome = Outcome.READY;
        String reason = null;

        if (name == null) {
            outcome = Outcome.FAIL;
            reason = "필수값 누락: 이름";
        } else if (address == null) {
            outcome = Outcome.FAIL;
            reason = "필수값 누락: 주소";
        } else if (lat == null || lng == null) {
            outcome = Outcome.FAIL;
            reason = "좌표 없음 또는 변환 실패";
        } else if (hours != null && hours.contains("휴업") && !containsThisYear(hours)) {
            outcome = Outcome.EXCLUDE;
            reason = "휴업 표기(올해 아님): " + hours.replaceAll("\\s+", " ");
        }

        return new PublicRestaurantRow(source, name, address, phone, description, menu, hours, image,
                lat, lng, rawCategory, cat.category(), cat.reason(), cat.unmapped(), outcome, reason,
                dataDate);   // [추가]2026.11.11권성애 (수정: dataDate 추가)
    }

    // ── 정리 도우미 ─────────────────────────────────────────────

    // 한 줄 값: 탭·줄바꿈을 공백으로, 연속 공백 1칸, 앞뒤 공백 제거. 비었거나 "null"이면 null
    private String oneLine(Object value) {
        if (value == null) return null;
        String s = value.toString().replaceAll("[\\t\\r\\n]+", " ").replaceAll(" {2,}", " ").trim();
        return (s.isEmpty() || "null".equalsIgnoreCase(s)) ? null : s;
    }

    // 여러 줄 값(소개글·영업시간·메뉴): 줄바꿈은 살리고, 탭 제거, 줄마다 앞뒤 공백 제거
    private String multiLine(Object value) {
        if (value == null) return null;
        String s = value.toString().replace("\r", "").replace("\t", "");
        StringBuilder sb = new StringBuilder();
        for (String line : s.split("\n")) {
            String t = line.trim();
            if (!t.isEmpty()) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(t);
            }
        }
        String result = sb.toString();
        return result.isEmpty() || "null".equalsIgnoreCase(result) ? null : result;
    }

    // 주소 3단계 통일
    // [추가]2026.11.11권성애 (수정: 영문 꼬리 지우기 · "부산시" 처리 · 구군 끼워 넣기 추가 — 위 2-1 ~ 2-3)
    private String normalizeAddress(String address, String gugun) {
        if (address == null) return null;
        String a = ENGLISH_TAIL.matcher(address).replaceAll("").trim();          // 2-1
        if (a.isEmpty()) return null;

        String rest;                                                             // "부산광역시 " 뒤쪽
        if (a.startsWith("부산광역시 ")) rest = a.substring("부산광역시 ".length());
        else if (a.startsWith("부산시 ")) rest = a.substring("부산시 ".length());  // 2-2
        else if (a.startsWith("부산 ")) rest = a.substring("부산 ".length());
        else rest = a;
        rest = rest.trim();
        if (rest.startsWith("부산시 ")) rest = rest.substring("부산시 ".length()).trim();   // 2-2 "부산광역시 부산시 …"

        String g = (gugun == null) ? null : gugun.trim();                        // 2-3
        if (!startsWithGugun(rest) && g != null && GUGUN.contains(g)) {
            rest = g + " " + rest;
        }
        return "부산광역시 " + rest;
    }

    // [추가]2026.11.11권성애 주소에 구군이 들어 있는지 ("부산광역시 수영구 …") — 데이터 점검 "구 없음" 카운트용
    public boolean hasGugun(String address) {
        if (address == null || !address.startsWith("부산광역시 ")) return false;
        return startsWithGugun(address.substring("부산광역시 ".length()));
    }

    // "수영구 광남로…"처럼 16개 구군 이름 + 공백으로 시작하는지
    private boolean startsWithGugun(String rest) {
        for (String g : GUGUN) {
            if (rest.startsWith(g + " ")) return true;
        }
        return false;
    }

    // 이미지 주소: 경로만 오면 비짓부산 주소를 붙임
    private String imageUrl(String value) {
        if (value == null) return null;
        if (value.startsWith("http://") || value.startsWith("https://")) return value;
        if (value.startsWith("/")) return VISITBUSAN + value;
        return null;
    }

    // 좌표: 128.95 같은 숫자 · "35.16" 같은 문자열 모두 처리, 0이면 없음으로 봄
    private Double toDouble(Object value) {
        if (value == null) return null;
        try {
            double d = (value instanceof Number n) ? n.doubleValue() : Double.parseDouble(value.toString().trim());
            return (d == 0.0) ? null : d;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 올해 연도가 "2026" 또는 "26."(예: 26.03.12) 형태로 들어 있는지
    private boolean containsThisYear(String text) {
        int year = LocalDate.now(SEOUL).getYear();
        String yy = String.format("%02d", year % 100);
        Pattern p = Pattern.compile("(?<!\\d)" + year + "(?!\\d)|(?<!\\d)" + yy + "\\s*\\.");
        return p.matcher(text).find();
    }
}
