package com.example.mymatzip.dto.publicapi;

import com.example.mymatzip.entity.Category;
import com.example.mymatzip.entity.Source;

// ============================================================
// [PublicRestaurantRow] 공공 API 1건을 정리한 결과 (저장 직전 모양)
// ============================================================
// PublicRestaurantMapper가 API 응답(Map) 1건을 받아
//   trim · 주소 통일 · 이미지 주소 · 좌표 변환 · 카테고리 분류를 끝낸 뒤 이 객체로 돌려줍니다.
//
// outcome  : READY(저장 대상) / EXCLUDE(휴업 등으로 제외) / FAIL(필수값 누락 등)
// reason   : EXCLUDE · FAIL 이유
// rawCategory   : 원본 분류 값 (모범음식점 bsnsCond, 부산맛집은 null)
// categoryReason: 분류 근거 (예: "bsnsCond=한식", "키워드: 밥")
// unmapped : 매핑표 · 키워드로 분류하지 못해 ETC로 넣은 경우 true → [매핑없음] 기록
// [추가]2026.11.11권성애 dataDate : 데이터 기준일 (모범음식점 dataDay, 없으면 ovrdDate / 부산맛집은 null)
//                              → 같은 API 안 중복일 때 최신 기록을 남기는 기준
// [추가]2026.11.11권성애 comparingAddress() · isSameStoreName(): 같은 가게 비교 (이름 · 주소 표기 차이를 무시)
// ============================================================
public record PublicRestaurantRow(
        Source source,
        String name,
        String address,
        String phone,
        String description,
        String representativeMenu,
        String businessHours,
        String imageUrl,
        Double lat,
        Double lng,
        String rawCategory,
        Category category,
        String categoryReason,
        boolean unmapped,
        Outcome outcome,
        String reason,
        String dataDate          // [추가]2026.11.11권성애 데이터 기준일 (최신 기록 남기기용)
) {
    public enum Outcome { READY, EXCLUDE, FAIL }

    // 이름 + 주소 (중복 판단 키)
    public String key() {
        return name + "|" + address;
    }

    // [추가]2026.11.11권성애 (v3) 같은 가게 판단 = 비교용 주소가 같고 + isSameStoreName이 true
    //   DB 유니크 키(이름+주소)는 글자가 완전히 같을 때만 막으므로, 표기만 다른 같은 가게는 이 두 메서드로 찾음

    // [추가]2026.11.11권성애 비교용 주소: "…로/길 + 건물번호"까지만 남기고 공백 제거 (괄호 · 층 · 호 · 건물명 무시)
    //   예) "부산광역시 동구 조방로 39-12 (범일동)" = "부산광역시 동구 조방로39-12" → "부산광역시동구조방로39-12"
    //   "중앙대로179번길 16"처럼 '로' 바로 뒤 숫자가 '번길'로 이어지면 건너뛰고 '길 16'까지 감
    //   DB에는 저장하지 않음 (비교할 때만 잠깐 만드는 값)
    private static final java.util.regex.Pattern ROAD_NUMBER =
            java.util.regex.Pattern.compile("^(.*?(?:로|길)\\s*\\d+(?:-\\d+)?)(?=$|[\\s,(])");

    public static String comparingAddress(String address) {
        if (address == null) return "";
        java.util.regex.Matcher m = ROAD_NUMBER.matcher(address);
        String base = m.find() ? m.group(1) : address.replaceAll("\\(.*?\\)", "");   // 지번 주소 등은 괄호만 지움
        return base.replaceAll("\\s+", "");
    }

    // [추가]2026.11.11권성애 같은 가게 이름인지: 공백 빼고 소문자로 비교 → 같거나, 한쪽이 다른 쪽을 포함(짧은 쪽 3글자 이상)
    //   예) "이재모피자" / "이재모피자 본점" → true,  "랩24 바이 쿠무다" / "랩24바이쿠무다" → true
    //   3글자 미만은 포함 비교 안 함 ("회" · "집" 같은 짧은 이름이 아무 데나 걸리지 않게)
    public static boolean isSameStoreName(String a, String b) {
        if (a == null || b == null) return false;
        String x = a.replaceAll("\\s+", "").toLowerCase();
        String y = b.replaceAll("\\s+", "").toLowerCase();
        if (x.isEmpty() || y.isEmpty()) return false;
        if (x.equals(y)) return true;
        String shorter = (x.length() <= y.length()) ? x : y;
        String longer = (shorter == x) ? y : x;
        return shorter.length() >= 3 && longer.contains(shorter);
    }
}
