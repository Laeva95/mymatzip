package com.example.mymatzip.service.publicapi;

import com.example.mymatzip.entity.Category;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// ============================================================
// [PublicCategoryMapper] 공공 API 데이터 → 우리 Category 분류
// ============================================================
// 1) 모범음식점: bsnsCond(업태) 매핑표 (MODEL_TABLE)
//      - "한식"은 대표메뉴 · 업소명으로 해산물·회 / 국밥·탕 / 고기·구이를 한 번 더 나눕니다.
//      - 표에 없는 업태 → 키워드로 추정, 그래도 없으면 ETC (둘 다 [매핑없음] 기록)
//      // [추가]2026.11.11권성애 분식 · 기타 · 업태 없음 → 키워드를 먼저 보고(밀면 → 한식 등), 없으면 ETC
//                                 키워드로 분류되면 [매핑없음]에서 뺌 (업태 없음 + 키워드 없음만 [매핑없음])
//      // [추가]2026.11.11권성애 키워드는 업소명을 먼저, 대표메뉴는 나중에 검사 (예: 제일횟집 메뉴의 "일식"보다 이름의 "횟집")
//      ※ preview를 돌려 rawCategoryCounts에 나온 업태를 보고 MODEL_TABLE에 추가하면 됩니다.
// 2) 부산맛집: 분류 값이 없어 키워드로 추정 (대표메뉴 + 제목 → 없으면 소개글)
//
// 키워드 검사 순서가 중요합니다 (위에서부터 먼저 맞는 것):
//   카페 → 중식 → 일식 → 양식 → 해산물·회 → 국밥·탕 → 고기·구이 → 한식
//   예) "탕수육"은 국밥·탕(탕)보다 중식이 먼저라 중식, "갈비탕"은 고기·구이보다 국밥·탕이 먼저라 국밥·탕
// ============================================================
@Component
public class PublicCategoryMapper {

    // 분류 결과: 카테고리, 근거, 매핑 실패 여부
    public record Result(Category category, String reason, boolean unmapped) {}

    // ── 모범음식점 업태(bsnsCond) 매핑표 ──
    private static final Map<String, Category> MODEL_TABLE = new LinkedHashMap<>();
    static {
        MODEL_TABLE.put("한식", Category.KOREAN);
        MODEL_TABLE.put("일식", Category.JAPANESE);
        MODEL_TABLE.put("중국식", Category.CHINESE);
        MODEL_TABLE.put("중식", Category.CHINESE);
        MODEL_TABLE.put("경양식", Category.WESTERN);
        MODEL_TABLE.put("양식", Category.WESTERN);
        MODEL_TABLE.put("회집", Category.SEAFOOD);
        MODEL_TABLE.put("횟집", Category.SEAFOOD);
        MODEL_TABLE.put("복어취급", Category.SEAFOOD);
        MODEL_TABLE.put("식육(숯불구이)", Category.MEAT);
        MODEL_TABLE.put("탕류(보신용)", Category.SOUP);
        MODEL_TABLE.put("까페", Category.CAFE);
        MODEL_TABLE.put("카페", Category.CAFE);
        MODEL_TABLE.put("커피숍", Category.CAFE);
        MODEL_TABLE.put("다방", Category.CAFE);
        // [추가]2026.11.11권성애 첫 preview(268건)에서 나온 업태 추가
        MODEL_TABLE.put("식육", Category.MEAT);                       // 식육(숯불구이)와 같은 뜻
        MODEL_TABLE.put("복어", Category.SEAFOOD);                    // 복어취급과 같은 뜻
        MODEL_TABLE.put("중국", Category.CHINESE);
        MODEL_TABLE.put("패스트푸드", Category.WESTERN);              // 피자 · 버거
        MODEL_TABLE.put("외국음식전문점(인도,태국등)", Category.ETC);  // 분류 없음 → 기타
        MODEL_TABLE.put("뷔페식", Category.ETC);
    }

    // [추가]2026.11.11권성애 키워드를 먼저 보고, 없으면 ETC로 저장하는 업태 (팀 결정: 분식 → 기타, 단 밀면 · 냉면 등은 키워드로)
    private static final java.util.Set<String> KEYWORD_FIRST = java.util.Set.of("분식", "기타");

    // ── 키워드표 (순서 = 검사 순서) ──
    private static final Map<Category, List<String>> KEYWORDS = new LinkedHashMap<>();
    static {
        // [추가]2026.11.11권성애 (수정: preview CSV 오분류 · 매핑없음 보고 키워드 추가 — 줄 끝 주석)
        KEYWORDS.put(Category.CAFE, List.of("커피", "카페", "케이크", "디저트", "베이커리", "라떼", "빙수", "브레드",
                "파티세리", "과자", "베이글", "bread", "빵"));                         // 추가: 파티세리 ~ 빵
        KEYWORDS.put(Category.CHINESE, List.of("짬뽕", "짜장", "탕수육", "중식", "중국", "딤섬", "마라", "양장피",
                "북경", "반점", "중화", "완탕", "교자", "홍콩"));                       // 추가: 북경 ~ 홍콩
        KEYWORDS.put(Category.JAPANESE, List.of("스시", "초밥", "사시미", "라멘", "우동", "돈까스", "돈카츠", "오마카세", "이자카야", "일식", "소바",
                "야키토리", "텐푸라", "쇼쿠도", "돈부리", "텐동", "규동"));              // 추가: 야키토리 ~ 규동
        KEYWORDS.put(Category.WESTERN, List.of("파스타", "피자", "스테이크", "이탈리", "브런치", "버거", "양식", "리조또",
                "프렌치", "프랑스", "비스트로"));                                       // 추가: 프렌치 ~ 비스트로
        KEYWORDS.put(Category.SEAFOOD, List.of("횟집", "회센터", "생선회", "물회", "회덮밥", "해산물", "조개", "장어", "곰장어", "꼼장어",
                "대게", "꽃게", "낙지", "아귀", "생선", "갈치", "복국", "복어", "자리돔", "재첩", "대구", "멍게", "전복",
                "해물", "쭈꾸미", "주꾸미", "회"));                                     // 추가: 해물 ~ 주꾸미 ("회"는 맨 끝 유지)
        KEYWORDS.put(Category.SOUP, List.of("국밥", "해장국", "곰탕", "설렁탕", "감자탕", "삼계탕", "갈비탕", "추어탕", "찌개", "시락국",
                "육개장", "탕"));                                                       // 추가: 육개장
        KEYWORDS.put(Category.MEAT, List.of("갈비", "삼겹", "목살", "고기", "숯불", "곱창", "대창", "막창", "한우", "불고기", "오리", "구이"));
        KEYWORDS.put(Category.KOREAN, List.of("한정식", "정식", "비빔", "밀면", "냉면", "칼국수", "백반", "나물", "김치", "찜", "보쌈", "족발",
                "국수", "완당", "어묵", "만두", "육회", "육전", "밥", "떡"));           // 추가: 국수 ~ 육전 (v3: 육회 · 육전)
    }

    // 한식을 다시 나눌 때 볼 카테고리
    private static final List<Category> KOREAN_REFINE = List.of(Category.SEAFOOD, Category.SOUP, Category.MEAT);

    /** 모범음식점: bsnsCond + 대표메뉴(menu) + 업소명 */
    public Result mapModel(String bsnsCond, String menu, String name) {
        String cond = (bsnsCond == null) ? null : bsnsCond.trim();      // [추가]2026.11.11권성애 빈 값 정리
        if (cond != null && cond.isEmpty()) cond = null;
        Category mapped = (cond == null) ? null : MODEL_TABLE.get(cond);
        List<Category> all = List.copyOf(KEYWORDS.keySet());

        if (mapped == Category.KOREAN) {
            // 한식 + 메뉴가 복국·국밥·갈비 등이면 새 분류로
            Match refine = findNameFirst(name, menu, KOREAN_REFINE);   // [추가]2026.11.11권성애 (수정: 업소명 먼저, 메뉴 나중)
            if (refine != null) {
                return new Result(refine.category(), "bsnsCond=한식 + 키워드: " + refine.keyword(), false);
            }
            return new Result(Category.KOREAN, "bsnsCond=한식", false);
        }
        if (mapped != null) {
            return new Result(mapped, "bsnsCond=" + cond, false);
        }

        // [추가]2026.11.11권성애 분식 · 기타 · 업태 없음: 키워드 먼저, 없으면 ETC
        //   분식 · 기타는 팀 결정대로 ETC가 정상 결과라 [매핑없음] 아님, 업태 없음 + 키워드 없음만 [매핑없음]
        if (cond == null || KEYWORD_FIRST.contains(cond)) {
            String label = (cond == null) ? "bsnsCond 없음" : "bsnsCond=" + cond;
            Match m = findNameFirst(name, menu, all);
            if (m != null) {
                return new Result(m.category(), label + " → 키워드: " + m.keyword(), false);
            }
            return new Result(Category.ETC, label + " → 키워드 없음", cond == null);
        }

        // 매핑표에 없는 업태 → 키워드 추정 (매핑표 보완 대상이라 unmapped=true)
        Match guess = findNameFirst(name, join(cond, menu), all);       // [추가]2026.11.11권성애 (수정: 업소명 먼저)
        if (guess != null) {
            return new Result(guess.category(), "bsnsCond=" + cond + "(표에 없음) → 키워드: " + guess.keyword(), true);
        }
        return new Result(Category.ETC, "bsnsCond=" + cond + "(표에 없음, 키워드 없음)", true);
    }

    // [추가]2026.11.11권성애 업소명에서 먼저 찾고, 없으면 메뉴(등)에서 찾기
    private Match findNameFirst(String name, String rest, List<Category> order) {
        Match m = findKeyword(name, order);
        return (m != null) ? m : findKeyword(rest, order);
    }

    /** 부산맛집: 첫 번째 대표메뉴 + 이름 → 대표메뉴 전체 + 제목 → 없으면 소개글 */
    public Result mapFood(String menu, String title, String name, String description) {
        List<Category> all = List.copyOf(KEYWORDS.keySet());
        // 1) 첫 번째 메뉴(주메뉴)와 가게 이름을 먼저 본다
        //    예) "돌솥곤드레정식, 단호박오리훈제" → 두 번째 메뉴의 "오리" 때문에 고기·구이로 가지 않게
        Match m = findKeyword(join(firstMenu(menu), name), all);
        if (m != null) {
            return new Result(m.category(), "주메뉴·이름 키워드: " + m.keyword(), false);
        }
        // 2) 대표메뉴 전체 + 제목
        m = findKeyword(join(menu, title), all);
        if (m != null) {
            return new Result(m.category(), "키워드: " + m.keyword(), false);
        }
        // 소개글은 문장이 길어 "회사"·"회식"의 '회'처럼 한 글자 키워드가 잘못 걸리기 쉬움 → 두 글자 이상만
        m = findKeyword(description, all, false);
        if (m != null) {
            return new Result(m.category(), "소개글 키워드: " + m.keyword(), false);
        }
        return new Result(Category.ETC, "키워드 없음", true);
    }

    // ── 키워드 찾기 ───────────────────────────────────────────
    private record Match(Category category, String keyword) {}

    private Match findKeyword(String text, List<Category> order) {
        return findKeyword(text, order, true);
    }

    private Match findKeyword(String text, List<Category> order, boolean allowOneLetter) {
        if (text == null || text.isBlank()) {
            return null;
        }
        // "OO회관"(식당 이름에 흔함)의 '회'가 해산물로 잡히지 않게 미리 지움
        // [추가]2026.11.11권성애 (수정: 영문 키워드(bread) 비교를 위해 소문자로)
        // [추가]2026.11.11권성애 (v3 수정: "육회"를 지우는 대신 "육전"으로 바꿈 → 한 글자 "회"(해산물)에 먼저 걸리지 않고 한식 "육전"에 걸림)
        String target = text.replace("회관", "").replace("육회", "육전").toLowerCase();
        for (Category c : order) {
            for (String k : KEYWORDS.get(c)) {
                if (!allowOneLetter && k.length() < 2) {
                    continue;
                }
                if (target.contains(k)) {
                    return new Match(c, k);
                }
            }
        }
        return null;
    }

    // "돌솥곤드레정식, 단호박오리훈제" → "돌솥곤드레정식"
    private String firstMenu(String menu) {
        if (menu == null) return null;
        String first = menu.split("[,\\n/]")[0].trim();
        return first.isEmpty() ? null : first;
    }

    private String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                sb.append(p).append(' ');
            }
        }
        return sb.toString().trim();
    }
}
