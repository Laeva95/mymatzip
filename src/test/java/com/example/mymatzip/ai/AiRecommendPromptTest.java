package com.example.mymatzip.ai;

import com.example.mymatzip.dto.RestaurantResponse;
import com.example.mymatzip.entity.Category;
import com.example.mymatzip.service.HomeService;
import com.example.mymatzip.service.RestaurantService;
import com.example.mymatzip.service.ai.AiRecommendService;
import com.example.mymatzip.service.ai.AiUsageLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

// 검증일지 25번: AI 추천 프롬프트·조건 처리 (실제 AI 는 부르지 않고 응답을 정해 둠)
class AiRecommendPromptTest {

    private final RestaurantResponse soup = RestaurantResponse.builder()
            .id(1L).name("국밥|집").category(Category.SOUP).categoryDisplayName("국밥·탕")
            .address("부산광역시 해운대구 해운대해변로 1").description("진한 국물\n돼지국밥").representativeMenu("돼지국밥 ￦10,000\n수육 ￦20,000")
            .averageRating(0.0).createdAt(LocalDateTime.now()).build();

    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private AiUsageLimiter limiter;
    private AiRecommendService service;

    private void setUp(String aiAnswer) {
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(aiAnswer);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(chatClient);
        RestaurantService restaurantService = mock(RestaurantService.class);
        when(restaurantService.findAllAvoidNPlusOne()).thenReturn(List.of(soup));
        limiter = new AiUsageLimiter(2);
        service = new AiRecommendService(builder, restaurantService, limiter, mock(HomeService.class));
    }

    @Test
    void 프롬프트에_지역과_대표메뉴가_한_줄로_들어간다() {
        String prompt = AiRecommendService.buildSystemPrompt(List.of(soup));
        assertThat(prompt).contains("번호|이름|카테고리|지역|평균별점|대표메뉴|설명");
        // 이름의 | 와 메뉴·설명의 줄바꿈이 후보 한 줄 형식을 깨지 않는다
        assertThat(prompt).contains("1|국밥/집|국밥·탕|해운대구 해운대해변로|0.0|돼지국밥 ￦10,000, 수육 ￦20,000|진한 국물, 돼지국밥");
        assertThat(prompt).contains("\"없음\"");
        assertThat(prompt).contains("목록에 없는 분위기·가격·위치를 지어내지 않는다");
    }

    @Test
    void 조건에_맞는_맛집이_없으면_AI_를_부르지_않고_횟수도_그대로() {
        setUp("1|아무 이유");
        Map<String, Object> result = service.recommend("중구 카페 추천", 1L);
        assertThat((List<?>) result.get("picks")).isEmpty();
        assertThat(result.get("remaining")).isEqualTo(2);
        verify(chatClient, never()).prompt();
    }

    @Test
    void AI_가_없음_이라고_답하면_빈_결과_횟수_복구() {
        setUp("없음");
        Map<String, Object> result = service.recommend("해운대 국밥", 1L);
        assertThat((List<?>) result.get("picks")).isEmpty();
        assertThat(result.get("remaining")).isEqualTo(2);
        verify(chatClient).prompt();
    }

    @Test
    void 정상_응답은_후보_맛집과_이유로_반환() {
        setUp("1|해운대구에 있는 돼지국밥 전문점이다.\n99|목록에 없는 번호");
        Map<String, Object> result = service.recommend("해운대 국밥", 1L);
        List<?> picks = (List<?>) result.get("picks");
        assertThat(picks).hasSize(1);
        Map<?, ?> pick = (Map<?, ?>) picks.get(0);
        assertThat(pick.get("restaurant")).isSameAs(soup);
        assertThat(pick.get("reason")).isEqualTo("해운대구에 있는 돼지국밥 전문점이다.");
        assertThat(result.get("remaining")).isEqualTo(1);
    }
}
