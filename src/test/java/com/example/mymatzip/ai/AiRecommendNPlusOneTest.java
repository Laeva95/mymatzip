package com.example.mymatzip.ai;

import com.example.mymatzip.dto.RestaurantResponse;
import com.example.mymatzip.entity.Category;
import com.example.mymatzip.entity.Member;
import com.example.mymatzip.entity.Restaurant;
import com.example.mymatzip.entity.Review;
import com.example.mymatzip.entity.Role;
import com.example.mymatzip.entity.Source;
import com.example.mymatzip.repository.MemberRepository;
import com.example.mymatzip.repository.RestaurantRepository;
import com.example.mymatzip.repository.ReviewRepository;
import com.example.mymatzip.service.HomeService;
import com.example.mymatzip.service.RestaurantService;
import com.example.mymatzip.service.ai.AiRecommendService;
import com.example.mymatzip.service.ai.AiUsageLimiter;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AI 추천(AiRecommendService.recommend)의 N+1 개선 효과 측정
 *
 * 기존 코드  : recommend() → restaurantService.findAll()
 *              → 맛집 목록 1번 + 맛집마다 평균 별점 1번  = 1 + N 쿼리
 * 현재 코드  : recommend() → restaurantService.findAllAvoidNPlusOne()  (2026.11.17 김지현 커밋 d6b836f 에서 적용)
 *              → 맛집 목록 1번 + 평균 별점 IN 쿼리 1번   = 2 쿼리
 *
 * 측정 범위: "후보 목록 조회" 구간만. AI 가 고른 맛집(최대 3곳)에 리뷰 수 · 찜 수를 채우는
 *   HomeService.findByIdsWithReviewCountAndWishlist()(쿼리 3번 고정)는 mock 으로 넘기고,
 *   AI 응답도 빈 문자열이라 호출되지 않는다. 그 쿼리 수는 HomeServiceQueryCountTest ④ 에서 따로 검증한다.
 *
 * recommend() 에 넘기는 RestaurantService 를 "맛집 전체 목록을 요청하면 기존/개선 메서드로 연결"하는
 * 대역(mock)으로 바꿔서 비교한다. (서비스가 findAll / findAllAvoidNPlusOne 어느 쪽을 불러도 동작)
 * AI 호출(ChatClient)은 mock 이라 바로 빈 응답을 돌려주고, DB 구간만 측정된다.
 *
 * DB 는 H2 메모리라 쿼리 1번이 매우 빠르다. 실제 RDS 는 쿼리마다 네트워크 왕복이 붙으므로
 * StatementInspector 로 "쿼리마다 RDS_LATENCY_MS 만큼 대기"하는 시뮬레이션도 함께 잰다.
 *   (이 PC → busan-food-db TCP 연결 왕복 측정값 중앙값 ≈ 10ms)
 */
@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.hibernate.session_factory.statement_inspector="
                + "com.example.mymatzip.ai.AiRecommendNPlusOneTest$NetworkLatencyInspector",
        "logging.level.org.hibernate.orm.jdbc.bind=INFO",          // 바인딩 TRACE 로그가 수천 줄 찍히지 않게
        "spring.jpa.show-sql=false"
})
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AiRecommendNPlusOneTest {

    private static final int[] RESTAURANT_COUNTS = {100, 300, 1000};  // 맛집 수를 늘려가며 측정
    private static final long RDS_LATENCY_MS = 10;                     // 쿼리 1번당 가정한 RDS 왕복 시간
    private static final String KEYWORD = "AI엔플원";
    private static final String QUESTION = "해운대 국밥 맛집 추천해줘";

    private static final int WARMUP = 3;
    private static final int REPEAT_H2 = 5;    // H2(지연 없음) 평균 횟수
    private static final int REPEAT_RDS = 1;   // RDS 지연 시뮬레이션은 오래 걸려서 1회

    @Autowired private RestaurantService restaurantService;
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;
    private Member member;
    private final List<Restaurant> restaurants = new ArrayList<>();
    private final List<Review> reviews = new ArrayList<>();
    private final List<String> resultLines = new ArrayList<>();

    /** 쿼리가 준비될 때마다 호출됨 → latencyMs 만큼 잠들어 네트워크 왕복을 흉내낸다 */
    public static class NetworkLatencyInspector implements StatementInspector {
        static volatile long latencyMs = 0;

        @Override
        public String inspect(String sql) {
            if (latencyMs > 0) {
                try {
                    Thread.sleep(latencyMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return sql;
        }
    }

    @BeforeAll
    void setUp() {
        NetworkLatencyInspector.latencyMs = 0;
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);

        member = memberRepository.save(Member.builder()
                .email("ai-nplusone@test.com")
                .password("encodedPassword123!")
                .nickname("AI엔플원테스터")
                .role(Role.USER)
                .build());
    }

    @AfterAll
    void tearDown() {
        NetworkLatencyInspector.latencyMs = 0;
        reviewRepository.deleteAllInBatch(reviews);
        restaurantRepository.deleteAllInBatch(restaurants);
        memberRepository.delete(member);

        System.out.println();
        System.out.println("==============================================================================================");
        System.out.printf(" AI 추천 N+1 비교  (recommend() 1회 기준, RDS 지연 시뮬레이션 = 쿼리당 %dms)%n", RDS_LATENCY_MS);
        System.out.println("----------------------------------------------------------------------------------------------");
        System.out.printf(" %6s | %8s | %8s | %10s | %10s | %12s | %12s | %7s%n",
                "맛집수", "기존쿼리", "개선쿼리", "기존 H2ms", "개선 H2ms", "기존 RDS ms", "개선 RDS ms", "RDS배율");
        System.out.println("----------------------------------------------------------------------------------------------");
        resultLines.forEach(System.out::println);
        System.out.println("==============================================================================================");
        System.out.println();
    }

    @Test
    @DisplayName("AI 추천: findAll() → findAllAvoidNPlusOne() 로 바꿨을 때 쿼리 수 · 시간 비교")
    void compareRecommend() {
        for (int target : RESTAURANT_COUNTS) {
            addRestaurantsUpTo(target);

            // 두 방식이 AI 에 넘기는 맛집 목록(id, 평균 별점)이 완전히 같은지 먼저 확인
            assertThat(summary(restaurantService.findAllAvoidNPlusOne()))
                    .isEqualTo(summary(restaurantService.findAll()));

            AiRecommendService before = recommendServiceUsing(() -> restaurantService.findAll());
            AiRecommendService after = recommendServiceUsing(() -> restaurantService.findAllAvoidNPlusOne());

            Result oldR = measure(before);
            Result newR = measure(after);

            int total = restaurantService.findAllAvoidNPlusOne().size();
            assertThat(oldR.queries).isGreaterThan(total);        // 1 + N
            assertThat(newR.queries).isLessThanOrEqualTo(3);      // 목록 1 + IN 1 (+ 여유)

            String line = String.format(" %6d | %8d | %8d | %10.1f | %10.1f | %12.0f | %12.0f | %6.0fx",
                    total, oldR.queries, newR.queries,
                    oldR.h2Millis, newR.h2Millis, oldR.rdsMillis, newR.rdsMillis,
                    oldR.rdsMillis / newR.rdsMillis);
            resultLines.add(line);
            System.out.println("[AI 추천 N+1] " + line.trim());
        }
    }

    // ───────────────────────────── 도우미 ─────────────────────────────

    /**
     * 맛집 전체 목록 조회를 지정한 방식(기존 / 개선)으로 연결하는 RestaurantService 대역 + AI 는 즉시 빈 응답.
     * AiRecommendService 가 findAll() 과 findAllAvoidNPlusOne() 중 무엇을 부르든 같은 방식으로 실행되므로,
     * 서비스 코드를 고친 뒤에도 기존 / 개선 비교가 그대로 동작한다.
     */
    private AiRecommendService recommendServiceUsing(Supplier<List<RestaurantResponse>> loadAll) {
        RestaurantService fake = mock(RestaurantService.class);
        when(fake.findAll()).thenAnswer(inv -> loadAll.get());
        when(fake.findAllAvoidNPlusOne()).thenAnswer(inv -> loadAll.get());

        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("");

        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(chatClient);

        // 사용 횟수 제한에 걸리지 않도록 넉넉하게 (빈 응답이면 어차피 환불됨)
        return new AiRecommendService(builder, fake, new AiUsageLimiter(1_000_000), mock(HomeService.class));
    }

    private Result measure(AiRecommendService service) {
        NetworkLatencyInspector.latencyMs = 0;
        for (int i = 0; i < WARMUP; i++) {
            service.recommend(QUESTION, member.getId());
        }

        statistics.clear();
        service.recommend(QUESTION, member.getId());
        long queries = statistics.getPrepareStatementCount();

        double h2 = timeMillis(service, REPEAT_H2);

        NetworkLatencyInspector.latencyMs = RDS_LATENCY_MS;
        double rds = timeMillis(service, REPEAT_RDS);
        NetworkLatencyInspector.latencyMs = 0;

        return new Result(queries, h2, rds);
    }

    private double timeMillis(AiRecommendService service, int repeat) {
        long start = System.nanoTime();
        for (int i = 0; i < repeat; i++) {
            service.recommend(QUESTION, member.getId());
        }
        return (System.nanoTime() - start) / 1_000_000.0 / repeat;
    }

    /** 맛집을 target 개까지 추가 (5개 중 1개는 리뷰 없음, 나머지는 리뷰 1~4개) */
    private void addRestaurantsUpTo(int target) {
        Category[] categories = Category.values();
        List<Restaurant> newRestaurants = new ArrayList<>();
        for (int i = restaurants.size() + 1; i <= target; i++) {
            newRestaurants.add(Restaurant.builder()
                    .name(KEYWORD + " 국밥집 " + i)
                    .category(categories[i % categories.length])
                    .address("부산광역시 해운대구 AI엔플원로 " + i)
                    .description("AI 추천 N+1 테스트용 맛집")
                    .representativeMenu("돼지국밥")
                    .businessHours("11:00~21:00")
                    .source(Source.MANUAL)
                    .member(member)
                    .build());
        }
        restaurantRepository.saveAll(newRestaurants);

        List<Review> newReviews = new ArrayList<>();
        for (int k = 0; k < newRestaurants.size(); k++) {
            int i = restaurants.size() + k;
            if (i % 5 == 0) {
                continue;
            }
            for (int j = 0; j < (i % 4) + 1; j++) {
                newReviews.add(Review.builder()
                        .content("테스트 리뷰 " + i + "-" + j)
                        .rating((i + j) % 5 + 1)
                        .member(member)
                        .restaurant(newRestaurants.get(k))
                        .build());
            }
        }
        reviewRepository.saveAll(newReviews);

        restaurants.addAll(newRestaurants);
        reviews.addAll(newReviews);
    }

    private List<String> summary(List<RestaurantResponse> list) {
        return list.stream().map(r -> r.getId() + "=" + r.getAverageRating()).toList();
    }

    private record Result(long queries, double h2Millis, double rdsMillis) {
    }
}
