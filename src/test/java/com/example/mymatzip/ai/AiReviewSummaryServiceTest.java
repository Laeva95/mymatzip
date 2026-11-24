package com.example.mymatzip.ai;

import com.example.mymatzip.entity.Category;
import com.example.mymatzip.entity.Restaurant;
import com.example.mymatzip.entity.Review;
import com.example.mymatzip.repository.RestaurantRepository;
import com.example.mymatzip.repository.ReviewRepository;
import com.example.mymatzip.service.ai.AiReviewSummaryService;
import com.example.mymatzip.service.ai.AiUsageLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// AI 리뷰 요약 점검: 신고·AI 스팸 리뷰를 요약 대상에서 빼는지, 요약 메서드에 트랜잭션이 걸려 있지 않은지 확인 (실제 AI 는 부르지 않음)
class AiReviewSummaryServiceTest {

    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient chatClient;
    private ReviewRepository reviewRepository;
    private RestaurantRepository restaurantRepository;
    private AiUsageLimiter limiter;
    private AiReviewSummaryService service;
    private Restaurant restaurant;

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

        reviewRepository = mock(ReviewRepository.class);
        restaurantRepository = mock(RestaurantRepository.class);
        // 트랜잭션 매니저는 mock 이어도 TransactionTemplate 이 동작한다 (getTransaction 이 null 을 줘도 콜백은 실행됨)
        PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);

        restaurant = Restaurant.builder().name("테스트국밥").category(Category.SOUP).address("부산광역시 해운대구 해운대해변로 1").build();
        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(restaurant));

        limiter = new AiUsageLimiter(5);
        service = new AiReviewSummaryService(builder, reviewRepository, restaurantRepository, limiter, txManager);
    }

    private static Review review(String content, int rating, boolean reported, boolean spam) {
        Review r = new Review(content, rating, null, null, null);
        if (reported) r.report();
        if (spam) r.reportAsSpam("광고");
        return r;
    }

    @Test
    void 신고된_리뷰와_AI_스팸_리뷰는_요약에서_빠지고_남은_수로_3개_조건을_본다() {
        setUp("장점: - 국물\n아쉬운 점: - 짠맛\n한 줄 평: 괜찮다");
        // 정상 리뷰 2개 + 신고 1개 + 스팸 1개 → 쓸 수 있는 리뷰는 2개라서 AI 를 부르면 안 된다
        when(reviewRepository.findByRestaurantIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(
                review("국물이 진해요", 5, false, false),
                review("대출 상담 010-1234-5678", 1, true, false),
                review("짠맛이 강해요", 3, false, false),
                review("광고 링크 클릭", 1, false, true)));

        Map<String, Object> result = service.summarize(1L, 7L);

        assertThat(result.get("summary").toString()).contains("현재 2개");
        verify(chatClient, never()).prompt();
        assertThat(restaurant.getAiSummary()).isNull();
    }

    @Test
    void 신고된_리뷰는_AI_에게_보내는_글에_들어가지_않는다() {
        setUp("장점: - 국물\n아쉬운 점: - 짠맛\n한 줄 평: 괜찮다");
        when(reviewRepository.findByRestaurantIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(
                review("국물이 진해요", 5, false, false),
                review("대출 상담 010-1234-5678", 1, true, false),
                review("짠맛이 강해요", 3, false, false),
                review("맛있어요", 4, false, false)));

        service.summarize(1L, 7L);

        // 신고된 광고 문구는 프롬프트 어디에도 없어야 한다
        verify(requestSpec).user(org.mockito.ArgumentMatchers.argThat(
                (String s) -> !s.contains("대출 상담") && s.contains("국물이 진해요") && s.contains("맛있어요")));
    }

    @Test
    void 정상_리뷰_3개_이상이면_AI_요약을_저장한다() {
        setUp("장점: - 국물\n아쉬운 점: - 짠맛\n한 줄 평: 괜찮다");
        when(reviewRepository.findByRestaurantIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(
                review("국물이 진해요", 5, false, false),
                review("짠맛이 강해요", 3, false, false),
                review("맛있어요", 4, false, false)));

        Map<String, Object> result = service.summarize(1L, 7L);

        assertThat(result.get("cached")).isEqualTo(false);
        assertThat(result.get("summary").toString()).startsWith("장점: - 국물");
        assertThat(restaurant.getAiSummary()).isEqualTo(result.get("summary"));
        assertThat(result.get("remaining")).isEqualTo(4);
    }

    @Test
    void 저장된_요약이_있으면_리뷰를_읽지도_AI_를_부르지도_않는다() {
        setUp("사용하지 않는 답");
        restaurant.updateAiSummary("이미 저장된 요약");

        Map<String, Object> result = service.summarize(1L, 7L);

        assertThat(result.get("summary")).isEqualTo("이미 저장된 요약");
        assertThat(result.get("cached")).isEqualTo(true);
        verify(chatClient, never()).prompt();
        verify(reviewRepository, never()).findByRestaurantIdOrderByCreatedAtDesc(any());
    }

    @Test
    void 요약_메서드에는_트랜잭션_어노테이션이_없어_AI_대기_중_DB_연결을_잡지_않는다() throws Exception {
        Method summarize = AiReviewSummaryService.class.getMethod("summarize", Long.class, Long.class);
        assertThat(summarize.isAnnotationPresent(Transactional.class)).isFalse();
    }
}
