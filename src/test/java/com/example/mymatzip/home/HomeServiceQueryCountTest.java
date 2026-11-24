package com.example.mymatzip.home;

import com.example.mymatzip.dto.RestaurantResponse;
import com.example.mymatzip.entity.Category;
import com.example.mymatzip.entity.Member;
import com.example.mymatzip.entity.Restaurant;
import com.example.mymatzip.entity.Review;
import com.example.mymatzip.entity.Role;
import com.example.mymatzip.entity.Source;
import com.example.mymatzip.entity.Wishlist;
import com.example.mymatzip.repository.MemberRepository;
import com.example.mymatzip.repository.RestaurantRepository;
import com.example.mymatzip.repository.ReviewRepository;
import com.example.mymatzip.repository.WishlistRepository;
import com.example.mymatzip.service.HomeService;
import com.example.mymatzip.service.RestaurantService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// [HomeServiceQueryCountTest] 메인 화면 최근 맛집 6개 — 쿼리 수 · 개수 검증 (팀장 담당)
// ============================================================
// 대상: HomeService.findLatest6WithReviewCountAndWishlist() (HomeController.home() 이 사용)
//       HomeService.findByIdsWithReviewCountAndWishlist()   (AiRecommendService 가 AI 추천 카드에 사용)
//
// 확인하는 것
//   ① 쿼리 수가 3번으로 고정 (맛집 1 + 별점 · 리뷰 수 1 + 찜 수 1)
//      → 나중에 RestaurantResponse.from() 등에 연관 데이터 읽기가 추가돼 N+1 이 다시 생기면 이 테스트가 실패
//   ② 리뷰 수 · 찜 수 · 평균 별점이 실제로 넣은 데이터와 같음 (리뷰 · 찜이 없는 맛집은 0)
//   ③ 기존 findLatest6AvoidNPlusOne() 과 같은 맛집 · 같은 별점 (찜 수만 더해졌는지)
//
// 쿼리 수 세는 법: RestaurantServiceAvoidNPlusOneTest 와 같음
//   Hibernate Statistics 의 getPrepareStatementCount() = 실제로 DB 에 보낸 SQL 수
//   화면 요청처럼 읽기 전용 트랜잭션 하나 안에서 실행 (LAZY 를 읽어도 예외 없이 쿼리가 세어지도록)
// ============================================================
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)   // @BeforeAll / @AfterAll 을 static 없이 쓰기 위해
class HomeServiceQueryCountTest {

    private static final int RESTAURANT_COUNT = 8;   // 6개보다 많이 넣어서 "최신 6개만" 나오는지도 확인
    private static final int MEMBER_COUNT = 3;       // 리뷰 · 찜을 맛집당 최대 3개까지 (회원마다 1개씩)

    @Autowired private HomeService homeService;
    @Autowired private RestaurantService restaurantService;
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private WishlistRepository wishlistRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private PlatformTransactionManager transactionManager;

    private Statistics statistics;
    private TransactionTemplate readOnlyTx;

    private final List<Member> members = new ArrayList<>();
    private final List<Restaurant> restaurants = new ArrayList<>();
    private final List<Review> reviews = new ArrayList<>();
    private final List<Wishlist> wishlists = new ArrayList<>();

    // 맛집 id → 넣은 리뷰 수 · 찜 수 · 별점 합계 (기대값 계산용)
    private final Map<Long, Long> expectedReviewCount = new HashMap<>();
    private final Map<Long, Long> expectedWishlistCount = new HashMap<>();
    private final Map<Long, Integer> ratingSum = new HashMap<>();

    @BeforeAll
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        readOnlyTx = new TransactionTemplate(transactionManager);
        readOnlyTx.setReadOnly(true);

        for (int m = 0; m < MEMBER_COUNT; m++) {
            members.add(Member.builder()
                    .email("home-count-" + m + "@test.com")
                    .password("encodedPassword123!")
                    .nickname("홈개수테스터" + m)
                    .role(Role.USER)
                    .build());
        }
        memberRepository.saveAll(members);

        Category[] categories = Category.values();
        for (int i = 0; i < RESTAURANT_COUNT; i++) {
            restaurants.add(Restaurant.builder()
                    .name("홈개수테스트식당 " + i)
                    .category(categories[i % categories.length])
                    .address("부산광역시 해운대구 홈개수로 " + i)
                    .description("메인 화면 쿼리 수 테스트용 맛집")
                    .representativeMenu("테스트메뉴")
                    .businessHours("11:00~21:00")
                    .source(Source.MANUAL)
                    .member(members.get(0))
                    .build());
        }
        restaurantRepository.saveAll(restaurants);

        // 맛집마다 리뷰 수 · 찜 수를 다르게 (0개인 맛집도 섞음)
        //   리뷰: i % 4 개 (0 ~ 3개), 찜: (i + 1) % 4 개 (0 ~ 3개) — 회원마다 최대 1개씩
        for (int i = 0; i < restaurants.size(); i++) {
            Restaurant restaurant = restaurants.get(i);
            int reviewTotal = Math.min(i % 4, MEMBER_COUNT);
            int wishTotal = Math.min((i + 1) % 4, MEMBER_COUNT);
            int sum = 0;
            for (int m = 0; m < reviewTotal; m++) {
                int rating = (i + m) % 5 + 1;
                sum += rating;
                reviews.add(Review.builder()
                        .content("홈 개수 테스트 리뷰 " + i + "-" + m)
                        .rating(rating)
                        .member(members.get(m))
                        .restaurant(restaurant)
                        .build());
            }
            for (int m = 0; m < wishTotal; m++) {
                wishlists.add(Wishlist.builder()
                        .member(members.get(m))
                        .restaurant(restaurant)
                        .build());
            }
            expectedReviewCount.put(restaurant.getId(), (long) reviewTotal);
            expectedWishlistCount.put(restaurant.getId(), (long) wishTotal);
            ratingSum.put(restaurant.getId(), sum);
        }
        reviewRepository.saveAll(reviews);
        wishlistRepository.saveAll(wishlists);
    }

    @AfterAll
    void tearDown() {
        // 다른 테스트 클래스와 같은 H2 DB 를 쓰므로 넣은 데이터는 모두 지움 (외래 키 순서: 찜 · 리뷰 → 맛집 → 회원)
        wishlistRepository.deleteAllInBatch(wishlists);
        reviewRepository.deleteAllInBatch(reviews);
        restaurantRepository.deleteAllInBatch(restaurants);
        memberRepository.deleteAllInBatch(members);
    }

    @Test
    @DisplayName("① 최근 맛집 6개 + 리뷰 수 · 찜 수 — 쿼리 3번 고정 (N+1 없음)")
    void queryCountIsThree() {
        List<RestaurantResponse> result = new ArrayList<>();
        long queries = countQueries(() -> result.addAll(homeService.findLatest6WithReviewCountAndWishlist()));

        System.out.println("[HomeService] findLatest6WithReviewCountAndWishlist() 결과 " + result.size()
                + "개, 쿼리 " + queries + "번");
        assertThat(result).hasSize(6);
        assertThat(queries)
                .as("맛집 1 + 별점 · 리뷰 수 1 + 찜 수 1 = 3번이어야 함 (더 많으면 카드를 만들 때 추가 쿼리 = N+1)")
                .isEqualTo(3);
    }

    @Test
    @DisplayName("② 리뷰 수 · 찜 수 · 평균 별점이 넣은 데이터와 같음 (없으면 0)")
    void countsMatchData() {
        List<RestaurantResponse> result = readOnlyTx.execute(
                status -> homeService.findLatest6WithReviewCountAndWishlist());

        assertThat(result).hasSize(6);
        for (RestaurantResponse r : result) {
            // 이 테스트가 넣은 맛집이 가장 최신이므로 6개 모두 여기서 넣은 맛집이어야 함
            assertThat(expectedReviewCount).as("최신 6개는 이 테스트가 넣은 맛집").containsKey(r.getId());

            long reviewCount = expectedReviewCount.get(r.getId());
            assertThat(r.getReviewCount()).as(r.getName() + " 리뷰 수").isEqualTo(reviewCount);
            assertThat(r.getWishlistCount()).as(r.getName() + " 찜 수").isEqualTo(expectedWishlistCount.get(r.getId()));

            // 평균 별점: RestaurantResponse.from() 과 같은 규칙 (소수점 1자리 반올림, 리뷰 없으면 0.0)
            double expectedAvg = reviewCount == 0 ? 0.0
                    : Math.round((double) ratingSum.get(r.getId()) / reviewCount * 10) / 10.0;
            assertThat(r.getAverageRating()).as(r.getName() + " 평균 별점").isEqualTo(expectedAvg);
        }
    }

    @Test
    @DisplayName("③ 기존 findLatest6AvoidNPlusOne() 과 같은 맛집 · 같은 별점 (찜 수 쿼리 1번만 늘어남)")
    void sameAsPreviousMethod() {
        List<RestaurantResponse> before = new ArrayList<>();
        List<RestaurantResponse> after = new ArrayList<>();
        long beforeQueries = countQueries(() -> before.addAll(restaurantService.findLatest6AvoidNPlusOne()));
        long afterQueries = countQueries(() -> after.addAll(homeService.findLatest6WithReviewCountAndWishlist()));

        System.out.println("[HomeService] 기존 findLatest6AvoidNPlusOne() 쿼리 " + beforeQueries
                + "번 → findLatest6WithReviewCountAndWishlist() 쿼리 " + afterQueries + "번");

        // 같은 맛집 6개 (등록 시각이 같은 맛집은 순서가 바뀔 수 있어서 id 로 비교)
        Map<Long, Double> beforeRatings = before.stream()
                .collect(Collectors.toMap(RestaurantResponse::getId, RestaurantResponse::getAverageRating));
        Map<Long, Double> afterRatings = after.stream()
                .collect(Collectors.toMap(RestaurantResponse::getId, RestaurantResponse::getAverageRating));
        assertThat(afterRatings).as("같은 맛집 · 같은 평균 별점").isEqualTo(beforeRatings);

        // 찜 수 쿼리 1번만 늘어남
        assertThat(afterQueries).isEqualTo(beforeQueries + 1);

        // 기존 메서드는 리뷰 수 · 찜 수를 채우지 않음 (그래서 홈 화면이 새 메서드로 바뀜)
        Map<Long, RestaurantResponse> beforeById = before.stream()
                .collect(Collectors.toMap(RestaurantResponse::getId, Function.identity()));
        assertThat(beforeById.values()).allSatisfy(r -> assertThat(r.getWishlistCount()).isNull());
    }

    @Test
    @DisplayName("④ AI 추천 맛집(id 목록) + 리뷰 수 · 찜 수 — 쿼리 3번 고정, 없는 id 는 빠짐")
    void findByIdsForAiPicks() {
        // AI 가 3곳을 고른 상황 + 없는 번호 하나 (AiRecommendService 는 못 찾은 id 면 원래 응답을 씀)
        List<Long> pickedIds = List.of(restaurants.get(1).getId(), restaurants.get(4).getId(),
                restaurants.get(7).getId(), -1L);

        Map<Long, RestaurantResponse> result = new HashMap<>();
        long queries = countQueries(() -> result.putAll(homeService.findByIdsWithReviewCountAndWishlist(pickedIds)));

        System.out.println("[HomeService] findByIdsWithReviewCountAndWishlist() 고른 맛집 " + result.size()
                + "곳, 쿼리 " + queries + "번");
        assertThat(queries)
                .as("맛집 + 작성자 1 (join fetch) + 별점 · 리뷰 수 1 + 찜 수 1 = 3번 (작성자를 따로 읽으면 N+1)")
                .isEqualTo(3);
        assertThat(result).hasSize(3).doesNotContainKey(-1L);
        for (RestaurantResponse r : result.values()) {
            assertThat(r.getReviewCount()).as(r.getName() + " 리뷰 수").isEqualTo(expectedReviewCount.get(r.getId()));
            assertThat(r.getWishlistCount()).as(r.getName() + " 찜 수").isEqualTo(expectedWishlistCount.get(r.getId()));
            assertThat(r.getMemberNickname()).isEqualTo(members.get(0).getNickname());
        }

        // 빈 목록이면 쿼리 없이 빈 결과
        assertThat(countQueries(() -> homeService.findByIdsWithReviewCountAndWishlist(List.of()))).isZero();
    }

    // 읽기 전용 트랜잭션 하나 안에서 실행하고, 그동안 DB 에 보낸 SQL 수를 돌려줌
    private long countQueries(Runnable action) {
        statistics.clear();
        readOnlyTx.executeWithoutResult(status -> action.run());
        return statistics.getPrepareStatementCount();
    }
}
