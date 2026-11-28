package com.example.mymatzip.review;

import com.example.mymatzip.dto.PageResponse;
import com.example.mymatzip.dto.ReviewResponse;
import com.example.mymatzip.entity.Category;
import com.example.mymatzip.entity.Member;
import com.example.mymatzip.entity.Restaurant;
import com.example.mymatzip.entity.Review;
import com.example.mymatzip.entity.Role;
import com.example.mymatzip.entity.Source;
import com.example.mymatzip.repository.MemberRepository;
import com.example.mymatzip.repository.RestaurantRepository;
import com.example.mymatzip.repository.ReviewRepository;
import com.example.mymatzip.service.ReviewService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 리뷰 목록 페이지 조회 시 ReviewResponse.from()이 접근하는
 * helpfulMemberIds / keywordTags 컬렉션의 실제 SQL 실행 횟수를 측정한다.
 *
 * Before 측정: Review.java의 두 @BatchSize(size = 50)를 잠시 주석 처리한 뒤 실행
 * After 측정 : 두 @BatchSize(size = 50)를 복원한 뒤 동일 테스트를 다시 실행
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReviewListQueryCountTest {

    private static final int TOTAL_REVIEW_COUNT = 12;
    private static final int PAGE_SIZE = 10;

    @Autowired private ReviewService reviewService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private PlatformTransactionManager transactionManager;

    private Statistics statistics;
    private TransactionTemplate readOnlyTx;
    private Member member;
    private Restaurant restaurant;
    private final List<Review> reviews = new ArrayList<>();

    @BeforeAll
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);

        readOnlyTx = new TransactionTemplate(transactionManager);
        readOnlyTx.setReadOnly(true);

        member = memberRepository.save(Member.builder()
                .email("review-list-count@test.com")
                .password("encodedPassword123!")
                .nickname("리뷰목록쿼리테스터")
                .role(Role.USER)
                .build());

        restaurant = restaurantRepository.save(Restaurant.builder()
                .name("리뷰목록쿼리테스트식당")
                .category(Category.KOREAN)
                .address("부산광역시 해운대구 리뷰목록쿼리테스트로 1")
                .description("리뷰 목록 쿼리 수 측정용")
                .representativeMenu("테스트메뉴")
                .businessHours("11:00~21:00")
                .source(Source.MANUAL)
                .member(member)
                .build());

        for (int i = 0; i < TOTAL_REVIEW_COUNT; i++) {
            Review review = Review.builder()
                    .content("리뷰 목록 쿼리 수 테스트 리뷰 " + i)
                    .rating(5)
                    .member(member)
                    .restaurant(restaurant)
                    .build();

            // 두 컬렉션 모두 비어 있지 않게 하여 DTO 변환 중 실제 조회가 발생하도록 함
            review.addHelpful(1001L);
            review.addHelpful(1002L);
            review.changeKeywordTags(List.of("음식이 맛있어요", "가성비가 좋아요"));
            reviews.add(review);
        }
        reviewRepository.saveAll(reviews);
    }

    @AfterAll
    void tearDown() {
        // ElementCollection 테이블까지 정리되도록 bulk delete가 아닌 엔티티 삭제 사용
        reviewRepository.deleteAll(reviews);
        restaurantRepository.delete(restaurant);
        memberRepository.delete(member);
    }

    @Test
    @DisplayName("리뷰 10건 페이지 조회 시 도움돼요·키워드 컬렉션 쿼리 수 측정")
    void reviewListQueryCount() {
        AtomicReference<PageResponse<ReviewResponse>> resultRef = new AtomicReference<>();

        long queries = countQueries(() -> resultRef.set(
                reviewService.findByRestaurantId(restaurant.getId(), PageRequest.of(0, PAGE_SIZE))));

        PageResponse<ReviewResponse> result = resultRef.get();
        System.out.println("[ReviewService] 리뷰 목록 " + result.getContent().size()
                + "건 / 전체 " + result.getTotalElements() + "건, 쿼리 " + queries + "번");

        assertThat(result.getContent()).hasSize(PAGE_SIZE);
        assertThat(result.getTotalElements()).isEqualTo(TOTAL_REVIEW_COUNT);
        assertThat(result.getContent()).allSatisfy(review -> {
            assertThat(review.getHelpfulCount()).isEqualTo(2);
            assertThat(review.getKeywordTags())
                    .containsExactlyInAnyOrder("음식이 맛있어요", "가성비가 좋아요");
        });
    }

    private long countQueries(Runnable action) {
        statistics.clear();
        readOnlyTx.executeWithoutResult(status -> action.run());
        return statistics.getPrepareStatementCount();
    }
}
