package com.example.mymatzip.service.ai;

import com.example.mymatzip.dto.RestaurantResponse;
import com.example.mymatzip.entity.Category;

import java.util.*;
import java.util.regex.Pattern;

// ============================================================
// [AiCandidateSelector] AI 추천에 보낼 후보 맛집 고르기 (D 찜 담당)
// ============================================================
// 왜 필요한가 (검증일지 25번)
//   예전에는 전체 맛집을 "평균 별점 → 최신 등록순" 으로 50개만 잘라 AI 에게 보냈다.
//   리뷰가 없으면 별점이 모두 0 이라 사실상 "최근 등록 50개" 가 되고,
//   그 50개에 없는 음식 종류·지역(예: 일식, 중구 카페)을 물으면 AI 가 엉뚱한 맛집을 억지로 골랐다.
//
// 하는 일
//   1) 질문에서 음식 종류(카테고리·대표 단어)와 지역(구·군, 서면·광안리 같은 동네 이름)을 찾는다
//   2) 찾은 조건을 "모두" 만족하는 맛집만 후보로 남긴다 (음식 종류끼리, 지역끼리는 하나라도 맞으면 됨)
//   3) 질문 단어가 이름·설명·대표메뉴에 많이 나오는 순 → 평균 별점 → 최신 등록순으로 정렬해 최대 N개
//   조건을 찾았는데 맞는 맛집이 하나도 없으면 빈 목록 → AI 를 부르지 않고 "찾지 못함" 으로 응답한다.
// static 으로 둔 이유: AI·DB 없이 AiCandidateSelectorTest 에서 바로 검사할 수 있게.
// ============================================================
public final class AiCandidateSelector {

    private AiCandidateSelector() {}

    /** 음식 종류 조건: 질문에 trigger 단어가 있으면, categories 에 속하거나 이름·설명·메뉴에 keywords 가 있는 맛집만 */
    private record FoodRule(List<String> triggers, Set<Category> categories, List<String> keywords) {}

    // 한 글자 단어(회, 탕 등)는 "회식", "탕수육" 처럼 잘못 걸리므로 넣지 않는다
    private static final List<FoodRule> FOOD_RULES = List.of(
            new FoodRule(List.of("한식", "한정식", "백반", "비빔밥", "찌개"),
                    EnumSet.of(Category.KOREAN), List.of("한식", "한정식", "백반", "비빔밥", "찌개")),
            new FoodRule(List.of("중식", "중국집", "중화", "짜장", "짬뽕", "탕수육", "마라", "딤섬"),
                    EnumSet.of(Category.CHINESE), List.of("중식", "중국", "중화", "짜장", "짬뽕", "탕수육", "마라", "딤섬")),
            new FoodRule(List.of("일식", "일본", "초밥", "스시", "라멘", "우동", "소바", "돈카츠", "돈까스", "오마카세", "이자카야"),
                    EnumSet.of(Category.JAPANESE), List.of("일식", "초밥", "스시", "라멘", "우동", "소바", "돈카츠", "돈까스", "오마카세", "이자카야")),
            new FoodRule(List.of("양식", "파스타", "피자", "스테이크", "버거", "리조또"),
                    EnumSet.of(Category.WESTERN), List.of("양식", "파스타", "피자", "스테이크", "버거", "리조또")),
            new FoodRule(List.of("카페", "커피", "디저트", "베이커리", "빵집", "케이크"),
                    EnumSet.of(Category.CAFE), List.of("카페", "커피", "디저트", "베이커리", "케이크")),
            new FoodRule(List.of("해산물", "해물", "횟집", "생선회", "조개", "장어", "복국", "꼼장어", "회센터"),
                    EnumSet.of(Category.SEAFOOD), List.of("해산물", "해물", "횟집", "생선회", "조개", "장어", "복국", "꼼장어")),
            new FoodRule(List.of("고기", "구이", "삼겹살", "갈비", "곱창", "소고기", "한우", "돼지고기", "숯불"),
                    EnumSet.of(Category.MEAT), List.of("고기", "구이", "삼겹살", "갈비", "곱창", "한우", "숯불")),
            new FoodRule(List.of("국밥", "국물", "해장국", "곰탕", "설렁탕", "갈비탕", "감자탕", "수육"),
                    EnumSet.of(Category.SOUP), List.of("국밥", "국물", "해장국", "곰탕", "설렁탕", "갈비탕", "감자탕", "탕면"))
    );

    // 주소에 구 이름이 그대로 없는 유명 동네 이름 → 구·군
    private static final Map<String, String> AREA_ALIASES = Map.ofEntries(
            Map.entry("서면", "부산진구"), Map.entry("전포", "부산진구"), Map.entry("부전", "부산진구"),
            Map.entry("광안리", "수영구"), Map.entry("광안", "수영구"), Map.entry("민락", "수영구"),
            Map.entry("남포", "중구"), Map.entry("자갈치", "중구"), Map.entry("광복동", "중구"), Map.entry("보수동", "중구"),
            Map.entry("센텀", "해운대구"), Map.entry("송정", "해운대구"), Map.entry("청사포", "해운대구"),
            Map.entry("달맞이", "해운대구"), Map.entry("마린시티", "해운대구"),
            Map.entry("부산대", "금정구"), Map.entry("장전", "금정구"),
            Map.entry("부산역", "동구"), Map.entry("초량", "동구"),
            Map.entry("경성대", "남구"), Map.entry("대연", "남구"),
            Map.entry("온천장", "동래구"), Map.entry("연산", "연제구"),
            Map.entry("하단", "사하구"), Map.entry("다대포", "사하구"),
            Map.entry("구포", "북구"), Map.entry("덕천", "북구"), Map.entry("명지", "강서구"),
            Map.entry("일광", "기장군"), Map.entry("송도", "서구")
    );

    // 질문 단어 점수에서 뺄 흔한 말
    private static final Set<String> STOP_WORDS = Set.of(
            "맛집", "추천", "추천해", "추천해줘", "추천해주세요", "알려줘", "해줘", "식당", "음식", "음식점",
            "먹을", "먹고", "싶어", "싶은", "있는", "좋은", "곳", "가게", "부산", "근처", "어디", "요리");

    private static final Pattern NON_WORD = Pattern.compile("[^0-9A-Za-z가-힣]+");
    private static final Pattern PARTICLE = Pattern.compile("(에서|으로|에게|까지|부터|이랑|하고|이나|은|는|이|가|을|를|의|에|로|도|만|랑|와|과)$");

    /** 질문에 맞는 후보를 최대 max 개 고른다. 조건이 있는데 맞는 맛집이 없으면 빈 목록. */
    public static List<RestaurantResponse> select(String question, List<RestaurantResponse> all, int max) {
        String q = question == null ? "" : question;

        List<FoodRule> foods = FOOD_RULES.stream()
                .filter(rule -> rule.triggers().stream().anyMatch(q::contains))
                .toList();
        Set<String> districts = detectDistricts(q, all);
        List<String> words = questionWords(q);

        Comparator<Scored> order = Comparator.comparingInt(Scored::score).reversed()
                .thenComparing(s -> s.r().getAverageRating(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(s -> s.r().getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(s -> s.r().getId(), Comparator.nullsLast(Comparator.naturalOrder()));

        return all.stream()
                .filter(r -> foods.isEmpty() || foods.stream().anyMatch(rule -> matchesFood(r, rule)))
                .filter(r -> districts.isEmpty() || districts.contains(districtOf(r.getAddress())))
                .map(r -> new Scored(r, score(r, words)))
                .sorted(order)
                .limit(max)
                .map(Scored::r)
                .toList();
    }

    /** 주소 "부산광역시 해운대구 해운대해변로 163" → "해운대구" (구·군이 없으면 빈 문자열) */
    public static String districtOf(String address) {
        if (address == null) return "";
        for (String token : address.trim().split("\\s+")) {
            if (token.length() >= 2 && (token.endsWith("구") || token.endsWith("군"))) return token;
        }
        return "";
    }

    /** 주소 → "해운대구 해운대해변로" 처럼 구·군 + 다음 한 단어 (AI 에게 보여 줄 지역) */
    public static String areaOf(String address) {
        if (address == null) return "";
        String[] tokens = address.trim().split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equals(districtOf(address))) {
                return i + 1 < tokens.length ? tokens[i] + " " + tokens[i + 1] : tokens[i];
            }
        }
        return "";
    }

    private record Scored(RestaurantResponse r, int score) {}

    private static boolean matchesFood(RestaurantResponse r, FoodRule rule) {
        if (r.getCategory() != null && rule.categories().contains(r.getCategory())) return true;
        String text = text(r);
        return rule.keywords().stream().anyMatch(text::contains);
    }

    private static Set<String> detectDistricts(String q, List<RestaurantResponse> all) {
        Set<String> known = new HashSet<>();
        for (RestaurantResponse r : all) {
            String d = districtOf(r.getAddress());
            if (!d.isEmpty()) known.add(d);
        }
        Set<String> found = new HashSet<>();
        for (String d : known) {
            // "중구"·"서구" 처럼 두 글자는 그대로 (앞 글자가 한글이면 "강서구" 의 일부이므로 제외)
            if (Pattern.compile("(?<![가-힣])" + Pattern.quote(d)).matcher(q).find()) found.add(d);
            // "해운대구" → "해운대" 처럼 구·군을 뗀 이름도 인정 (세 글자 이상만)
            if (d.length() >= 3 && q.contains(d.substring(0, d.length() - 1))) found.add(d);
        }
        AREA_ALIASES.forEach((alias, district) -> {
            if (q.contains(alias) && known.contains(district)) found.add(district);
        });
        return found;
    }

    private static List<String> questionWords(String q) {
        List<String> words = new ArrayList<>();
        for (String token : NON_WORD.split(q)) {
            String w = PARTICLE.matcher(token).replaceAll("");
            if (w.length() >= 3 && (w.endsWith("한") || w.endsWith("은"))) w = w.substring(0, w.length() - 1); // 조용한 → 조용
            if (w.length() >= 2 && !STOP_WORDS.contains(w) && !STOP_WORDS.contains(token)) words.add(w);
        }
        return words;
    }

    private static int score(RestaurantResponse r, List<String> words) {
        String text = text(r);
        int s = 0;
        for (String w : words) if (text.contains(w)) s++;
        return s;
    }

    private static String text(RestaurantResponse r) {
        return String.join(" ",
                Objects.toString(r.getName(), ""),
                Objects.toString(r.getCategoryDisplayName(), ""),
                Objects.toString(r.getAddress(), ""),
                Objects.toString(r.getDescription(), ""),
                Objects.toString(r.getRepresentativeMenu(), ""));
    }
}
