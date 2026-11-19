package com.example.mymatzip.restaurant;

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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================================
// [추가]2026.11.16권성애 RestaurantServiceAvoidNPlusOneTest
//   맛집 목록 N+1 — RestaurantService 의 기존 메서드 vs ...AvoidNPlusOne() 메서드 비교
// ============================================================================
//
// ▶ 무엇을 비교하나요? (둘 다 RestaurantService 의 실제 메서드)
//   [기존] findAll() 등 7개           : 맛집마다 평균 별점 쿼리 → 1 + N 번
//   [변경] findAllAvoidNPlusOne() 등 7개 : 평균 별점을 IN 쿼리로 한 번에 → 1 + 1 번 (페이징은 + 개수 1)
//
// ▶ 어떻게 재나요?
//   - 쿼리 수 : Hibernate Statistics 의 getPrepareStatementCount() (실제로 DB 에 보낸 SQL 수)
//   - 결과    : 두 방식의 (맛집 id, 평균 별점) 목록이 순서까지 똑같은지 확인 → "결과는 같고 쿼리만 줄었다"
//   - 시간(ms)은 재지 않음: H2 메모리 DB 라 오차가 커서 근거로 쓰기 어려움
//
// ▶ 주의
//   - @ActiveProfiles("test") → application-test.properties (H2 메모리 DB) 사용
//     절대 빼지 말 것: 빼면 application.properties(팀 공용 RDS, ddl create)로 실행됨
//   - 클래스 전체에 @Transactional 을 붙이지 않음
//     → 실제 요청처럼 측정마다 새 읽기 전용 트랜잭션(새 영속성 컨텍스트)에서 실행
//     → 그래서 @AfterAll 에서 넣은 데이터를 직접 지움
//   - application-test.properties 에 바인딩 TRACE 로그가 켜져 있으면 findAll 비교 때 값 줄이 아주 많이 찍힘
//     → 발표 캡처는 findLatest6Compare() 하나만 실행하면 깔끔함
// ============================================================================
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)   // @BeforeAll / @AfterAll 을 static 없이 쓰기 위해
class RestaurantServiceAvoidNPlusOneTest {

    // ── 테스트 데이터 양 (원하는 만큼 바꿔 보세요) ──
    private static final int RESTAURANT_COUNT = 100;   // 맛집 수
    private static final int PAGE_SIZE = 12;           // 목록 화면 한 페이지 크기 (app.restaurant.page-size 와 같게)
    private static final String KEYWORD = "엔플원회피";  // 검색어 (테스트 맛집 이름에 들어 있음, 기존 테스트와 겹치지 않게)

    @Autowired private RestaurantService restaurantService;
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private PlatformTransactionManager transactionManager;

    private Statistics statistics;
    private TransactionTemplate readOnlyTx;

    // 목록 화면(RestaurantViewController.list)과 같은 첫 페이지 조건: 최신 등록순, 같은 시각이면 id 내림차순
    private final Pageable firstPage = PageRequest.of(0, PAGE_SIZE,
            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));

    // 정리(@AfterAll)할 때 지울 데이터
    private Member member;
    private final List<Restaurant> restaurants = new ArrayList<>();
    private final List<Review> reviews = new ArrayList<>();

    // 콘솔에 마지막에 한꺼번에 찍을 결과 줄
    private final List<String> resultLines = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────
    // 데이터 준비: 맛집 100개, 5개 중 1개는 리뷰 없음(평균 별점 null), 나머지는 리뷰 1~4개
    // ─────────────────────────────────────────────────────────────
    @BeforeAll
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);

        readOnlyTx = new TransactionTemplate(transactionManager);
        readOnlyTx.setReadOnly(true);

        member = memberRepository.save(Member.builder()
                .email("avoid-nplusone@test.com")
                .password("encodedPassword123!")
                .nickname("엔플원회피테스터")
                .role(Role.USER)
                .build());

        Category[] categories = Category.values();
        for (int i = 1; i <= RESTAURANT_COUNT; i++) {
            restaurants.add(Restaurant.builder()
                    .name(KEYWORD + " 테스트식당 " + i)
                    .category(categories[i % categories.length])
                    .address("부산광역시 해운대구 엔플원회피로 " + i)
                    .description("N+1 비교 테스트용 맛집")
                    .representativeMenu("테스트메뉴")
                    .businessHours("11:00~21:00")
                    .source(Source.MANUAL)
                    .member(member)
                    .build());
        }
        restaurantRepository.saveAll(restaurants);

        for (int i = 0; i < restaurants.size(); i++) {
            if (i % 5 == 0) {
                continue;   // 리뷰 없는 맛집 → 평균 별점 null 도 두 방식이 같게 나오는지 확인
            }
            int reviewCount = (i % 4) + 1;
            for (int j = 0; j < reviewCount; j++) {
                reviews.add(Review.builder()
                        .content("테스트 리뷰 " + i + "-" + j)
                        .rating((i + j) % 5 + 1)
                        .member(member)
                        .restaurant(restaurants.get(i))
                        .build());
            }
        }
        reviewRepository.saveAll(reviews);
    }

    @AfterAll
    void tearDown() {
        reviewRepository.deleteAllInBatch(reviews);
        restaurantRepository.deleteAllInBatch(restaurants);
        memberRepository.delete(member);

        // ── 결과 표 출력 ──
        System.out.println();
        System.out.println("==========================================================================");
        System.out.printf(" 맛집 N+1 비교  (맛집 %d개, 리뷰 %d개, 페이지 크기 %d)%n",
                RESTAURANT_COUNT, reviews.size(), PAGE_SIZE);
        System.out.println("--------------------------------------------------------------------------");
        System.out.printf(" %-30s | %5s | %9s | %9s%n", "기존 메서드", "결과수", "기존 쿼리", "변경 쿼리");
        System.out.println("--------------------------------------------------------------------------");
        resultLines.forEach(System.out::println);
        System.out.println("==========================================================================");
        System.out.println(" ※ 변경 = 같은 이름 + AvoidNPlusOne (예: findAll() → findAllAvoidNPlusOne())");
        System.out.println(" ※ 결과가 한 페이지보다 적으면 스프링이 개수(count) 쿼리를 생략해서 페이징도 2번이 될 수 있음");
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────
    // 비교 7개: 기존 메서드 ↔ ...AvoidNPlusOne() 메서드
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("① findAll() ↔ findAllAvoidNPlusOne() — 전체 목록 (API)")
    void findAllCompare() {
        List<RestaurantResponse> oldList = new ArrayList<>();
        List<RestaurantResponse> newList = new ArrayList<>();

        long oldQueries = countQueries(() -> oldList.addAll(restaurantService.findAll()));
        long newQueries = countQueries(() -> newList.addAll(restaurantService.findAllAvoidNPlusOne()));

        assertSameRatings("findAll()", oldList, newList, oldQueries, newQueries);
    }

    @Test
    @DisplayName("② findAll(pageable) ↔ findAllAvoidNPlusOne(pageable) — 전체 목록 페이징 (화면)")
    void findAllPageCompare() {
        List<RestaurantResponse> oldList = new ArrayList<>();
        List<RestaurantResponse> newList = new ArrayList<>();

        long oldQueries = countQueries(() -> oldList.addAll(restaurantService.findAll(firstPage).getContent()));
        long newQueries = countQueries(() -> newList.addAll(restaurantService.findAllAvoidNPlusOne(firstPage).getContent()));

        assertSameRatings("findAll(page)", oldList, newList, oldQueries, newQueries);
    }

    @Test
    @DisplayName("③ findByCategory(category) ↔ findByCategoryAvoidNPlusOne(category) — 카테고리 목록 (API)")
    void findByCategoryCompare() {
        List<RestaurantResponse> oldList = new ArrayList<>();
        List<RestaurantResponse> newList = new ArrayList<>();

        long oldQueries = countQueries(() -> oldList.addAll(restaurantService.findByCategory(Category.KOREAN)));
        long newQueries = countQueries(() -> newList.addAll(restaurantService.findByCategoryAvoidNPlusOne(Category.KOREAN)));

        assertSameRatings("findByCategory(KOREAN)", oldList, newList, oldQueries, newQueries);
    }

    @Test
    @DisplayName("④ findByCategory(category, pageable) ↔ findByCategoryAvoidNPlusOne(category, pageable) — 카테고리 페이징 (화면)")
    void findByCategoryPageCompare() {
        List<RestaurantResponse> oldList = new ArrayList<>();
        List<RestaurantResponse> newList = new ArrayList<>();

        long oldQueries = countQueries(() -> oldList.addAll(
                restaurantService.findByCategory(Category.KOREAN, firstPage).getContent()));
        long newQueries = countQueries(() -> newList.addAll(
                restaurantService.findByCategoryAvoidNPlusOne(Category.KOREAN, firstPage).getContent()));

        assertSameRatings("findByCategory(KOREAN,page)", oldList, newList, oldQueries, newQueries);
    }

    @Test
    @DisplayName("⑤ search(keyword) ↔ searchAvoidNPlusOne(keyword) — 검색 (API)")
    void searchCompare() {
        List<RestaurantResponse> oldList = new ArrayList<>();
        List<RestaurantResponse> newList = new ArrayList<>();

        long oldQueries = countQueries(() -> oldList.addAll(restaurantService.search(KEYWORD)));
        long newQueries = countQueries(() -> newList.addAll(restaurantService.searchAvoidNPlusOne(KEYWORD)));

        assertSameRatings("search(keyword)", oldList, newList, oldQueries, newQueries);
    }

    @Test
    @DisplayName("⑥ search(keyword, pageable) ↔ searchAvoidNPlusOne(keyword, pageable) — 검색 페이징 (컨트롤러 미사용)")
    void searchPageCompare() {
        List<RestaurantResponse> oldList = new ArrayList<>();
        List<RestaurantResponse> newList = new ArrayList<>();

        long oldQueries = countQueries(() -> oldList.addAll(restaurantService.search(KEYWORD, firstPage).getContent()));
        long newQueries = countQueries(() -> newList.addAll(restaurantService.searchAvoidNPlusOne(KEYWORD, firstPage).getContent()));

        assertSameRatings("search(keyword,page)", oldList, newList, oldQueries, newQueries);
    }

    @Test
    @DisplayName("⑦ findLatest6() ↔ findLatest6AvoidNPlusOne() — 메인 최신 6개")
    void findLatest6Compare() {
        List<RestaurantResponse> oldList = new ArrayList<>();
        List<RestaurantResponse> newList = new ArrayList<>();

        long oldQueries = countQueries(() -> oldList.addAll(restaurantService.findLatest6()));
        long newQueries = countQueries(() -> newList.addAll(restaurantService.findLatest6AvoidNPlusOne()));

        assertSameRatings("findLatest6()", oldList, newList, oldQueries, newQueries);
    }

    // ─────────────────────────────────────────────────────────────
    // 도우미 메서드
    // ─────────────────────────────────────────────────────────────

    /**
     * 새 읽기 전용 트랜잭션에서 action 을 한 번 실행하고, 그동안 DB 로 보낸 SQL 수를 돌려줌
     *   - 실행 전에 statistics.clear() 로 0 부터 셈
     *   - 실제 요청처럼 매번 새 트랜잭션이라 1차 캐시 때문에 쿼리가 줄어드는 일이 없음
     */
    private long countQueries(Runnable action) {
        statistics.clear();
        readOnlyTx.executeWithoutResult(status -> action.run());
        return statistics.getPrepareStatementCount();
    }

    /**
     * 기존 / 변경 결과가 같은지 확인하고, 결과 표에 한 줄 기록
     *   1) 결과가 비어 있지 않아야 함 (데이터가 제대로 들어갔는지)
     *   2) 순서 · 맛집 id · 평균 별점이 완전히 같아야 함 → "결과는 같고 쿼리만 줄었다"
     *   3) 변경 쪽은 목록 1 + (페이징이면 개수 1) + 평점 1 → 최대 3번, 그리고 기존보다 적어야 함
     */
    private void assertSameRatings(String label,
                                   List<RestaurantResponse> oldList,
                                   List<RestaurantResponse> newList,
                                   long oldQueries,
                                   long newQueries) {
        assertThat(oldList).isNotEmpty();

        List<String> oldRatings = oldList.stream().map(r -> r.getId() + "=" + r.getAverageRating()).toList();
        List<String> newRatings = newList.stream().map(r -> r.getId() + "=" + r.getAverageRating()).toList();
        assertThat(newRatings).isEqualTo(oldRatings);

        assertThat(newQueries).isLessThanOrEqualTo(3);
        assertThat(newQueries).isLessThan(oldQueries);

        String line = String.format(" %-30s | %5d | %9d | %9d", label, oldList.size(), oldQueries, newQueries);
        resultLines.add(line);
        System.out.println("[N+1 비교] " + line.trim());
    }
}