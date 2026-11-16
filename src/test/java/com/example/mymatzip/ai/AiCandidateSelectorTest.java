package com.example.mymatzip.ai;

import com.example.mymatzip.dto.RestaurantResponse;
import com.example.mymatzip.entity.Category;
import com.example.mymatzip.service.ai.AiCandidateSelector;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 검증일지 25번: AI 추천 후보가 질문의 음식 종류·지역 조건을 반영하는지 (AI 를 부르지 않음)
class AiCandidateSelectorTest {

    private static RestaurantResponse r(long id, Category category, String address, String description, String menu, int daysAgo) {
        return RestaurantResponse.builder()
                .id(id).name("맛집" + id).category(category).categoryDisplayName(category.getDisplayName())
                .address(address).description(description).representativeMenu(menu)
                .averageRating(0.0).createdAt(LocalDateTime.now().minusDays(daysAgo))
                .build();
    }

    private final List<RestaurantResponse> all = List.of(
            r(1, Category.SOUP, "부산광역시 해운대구 해운대해변로 1", "돼지국밥 전문점", "돼지국밥 ￦10,000", 0),
            r(2, Category.MEAT, "부산광역시 부산진구 서면로 2", "숙성 사시미와 사케가 있는 곳", "사시미", 1),
            r(3, Category.JAPANESE, "부산광역시 수영구 광안해변로 3", "정통 스시 오마카세", "오마카세", 300),
            r(4, Category.CAFE, "부산광역시 해운대구 달맞이길 4", "바다가 보이는 조용한 카페", "아메리카노", 2),
            r(5, Category.SOUP, "부산광역시 부산진구 서전로 5", "진한 국물의 곰탕", "곰탕 ￦9,000", 3),
            r(6, Category.KOREAN, "부산광역시 강서구 공항로 6", "한정식", "한정식 코스", 4),
            r(7, Category.CHINESE, "부산광역시 서구 송도해변로 7", "짬뽕 맛집", "짬뽕", 5)
    );

    @Test
    void 음식_종류_조건이_있으면_그_종류만_후보로() {
        // 최근 등록순으로만 자르면 3번(일식, 300일 전)이 빠지고 2번(고기·구이)만 남던 문제
        assertThat(AiCandidateSelector.select("일식 맛집 추천해줘", all, 50))
                .extracting(RestaurantResponse::getId).containsExactly(3L);
    }

    @Test
    void 지역_조건이_있으면_그_지역만_후보로() {
        assertThat(AiCandidateSelector.select("해운대 맛집", all, 50))
                .extracting(RestaurantResponse::getId).containsExactlyInAnyOrder(1L, 4L);
        // 주소에 없는 동네 이름(서면) → 부산진구
        assertThat(AiCandidateSelector.select("서면에서 국물 요리", all, 50))
                .extracting(RestaurantResponse::getId).containsExactly(5L);
    }

    @Test
    void 두_글자_구_이름은_다른_구_이름의_일부와_섞이지_않는다() {
        assertThat(AiCandidateSelector.select("서구 맛집", all, 50))
                .extracting(RestaurantResponse::getId).containsExactly(7L);
        assertThat(AiCandidateSelector.select("강서구 맛집", all, 50))
                .extracting(RestaurantResponse::getId).containsExactly(6L);
    }

    @Test
    void 조건에_맞는_맛집이_없으면_빈_목록() {
        assertThat(AiCandidateSelector.select("강서구 카페 추천", all, 50)).isEmpty();
    }

    @Test
    void 조건이_없으면_질문_단어가_많이_맞는_순_그다음_최신순() {
        List<RestaurantResponse> picked = AiCandidateSelector.select("조용한 곳", all, 3);
        assertThat(picked).hasSize(3);
        assertThat(picked.get(0).getId()).isEqualTo(4L);            // 설명에 "조용" 이 있음
        assertThat(picked).extracting(RestaurantResponse::getId).containsExactly(4L, 1L, 2L);
    }

    @Test
    void 주소에서_구와_지역을_뽑는다() {
        assertThat(AiCandidateSelector.districtOf("부산광역시 해운대구 해운대해변로 163")).isEqualTo("해운대구");
        assertThat(AiCandidateSelector.districtOf("부산광역시 기장군 기장읍 1")).isEqualTo("기장군");
        assertThat(AiCandidateSelector.areaOf("부산광역시 해운대구 해운대해변로 163")).isEqualTo("해운대구 해운대해변로");
        assertThat(AiCandidateSelector.districtOf(null)).isEmpty();
    }

    @Test
    void 값이_비어_있는_맛집도_오류_없이_처리() {
        RestaurantResponse empty = RestaurantResponse.builder().id(9L).build();
        assertThat(AiCandidateSelector.select("일식", List.of(empty), 50)).isEmpty();
        assertThat(AiCandidateSelector.select("아무거나", List.of(empty), 50)).hasSize(1);
    }
}
