package com.example.mymatzip.restaurant;

import com.example.mymatzip.dto.PageResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================================
// [RestaurantNPlusOneTest] 맛집 목록 N+1 문제 — 기존 코드 vs toResponses() 비교
// ============================================================================
//
// ▶ 무엇을 비교하나요?
//   [기존] RestaurantService 의 실제 메서드 (맛집 1개마다 AVG 쿼리 → 1 + N 번)
//   [변경] 같은 Repository 조회 + 이 테스트 안의 toResponses() (AVG 를 IN 으로 한 번 → 1 + 1 번)
//
//   ※ 이 테스트는 N+1 을 "발견 · 측정"하려고 만든 것이라 실제 RestaurantService 는 고치지 않습니다.
//     "변경 후" 코드는 이 파일 아래쪽 toResponses() 에 그대로 옮겨 두었습니다.
//   ※ 이후 실제 수정은 RestaurantService 의 ...AvoidNPlusOne() 메서드로 들어갔습니다 (2026.11.16 권성애 커밋 fe9d85a).
//     그 메서드들의 쿼리 수 비교는 RestaurantServiceAvoidNPlusOneTest 가 검사합니다.
//     메인 화면 최근 맛집 6개는 지금 HomeService.findLatest6WithReviewCountAndWishlist()
//     (찜 수까지 포함, 쿼리 3번 고정)를 쓰며 HomeServiceQueryCountTest 가 검사합니다.
//
// ▶ 어떻게 재나요?
//   - 쿼리 수: Hibernate Statistics 의 getPrepareStatementCount() (실제로 DB 에 보낸 SQL 수)
//   - 시간   : 워밍업 WARMUP 번 후, REPEAT 번 실행한 평균 (ms)
//   - 결과   : 두 방식의 (맛집 id, 평균 별점) 목록이 똑같은지 assert 로 확인
//
// ▶ 주의
//   - H2 메모리 DB 라서 쿼리 1번이 매우 빠릅니다. 실제 MySQL(네트워크 왕복)에서는 시간 차이가 훨씬 커집니다.
//     → 시간보다 "쿼리 수" 를 보고 판단하세요.
//   - 클래스 전체에 @Transactional 을 붙이지 않았습니다.
//     실제 요청처럼 매 측정마다 새 트랜잭션(= 새 영속성 컨텍스트)에서 실행하기 위해서입니다.
//     그래서 마지막에 @AfterAll 로 넣은 데이터를 직접 지웁니다.
// ============================================================================
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)   // @BeforeAll / @AfterAll 을 static 없이 쓰기 위해
class RestaurantNPlusOneTest {

    // ── 테스트 데이터 양 (원하는 만큼 바꿔 보세요) ──
    private static final int RESTAURANT_COUNT = 100;   // 맛집 수
    private static final int PAGE_SIZE = 12;           // 목록 화면 한 페이지 크기
    private static final String KEYWORD = "엔플원";      // 검색어 (테스트 맛집 이름에 들어 있음)

    // ── 측정 횟수 ──
    private static final int WARMUP = 3;    // JIT · 쿼리 캐시가 데워지도록 먼저 버리는 실행
    private static final int REPEAT = 10;   // 평균을 낼 실행 횟수

    @Autowired private RestaurantService restaurantService;
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private PlatformTransactionManager transactionManager;

    private Statistics statistics;
    private TransactionTemplate readOnlyTx;

    // 정리(@AfterAll)할 때 지울 데이터
    private Member member;
    private final List<Restaurant> restaurants = new ArrayList<>();
    private final List<Review> reviews = new ArrayList<>();

    // 콘솔에 마지막에 한꺼번에 찍을 결과 줄
    private final List<String> resultLines = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────
    // 데이터 준비: 맛집 100개, 그중 5개 중 1개는 리뷰 없음, 나머지는 리뷰 1~4개
    // ─────────────────────────────────────────────────────────────
    @BeforeAll
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);

        readOnlyTx = new TransactionTemplate(transactionManager);
        readOnlyTx.setReadOnly(true);

        member = memberRepository.save(Member.builder()
                .email("nplusone@test.com")
                .password("encodedPassword123!")
                .nickname("엔플원테스터")
                .role(Role.USER)
                .build());

        Category[] categories = Category.values();
        for (int i = 1; i <= RESTAURANT_COUNT; i++) {
            restaurants.add(Restaurant.builder()
                    .name(KEYWORD + " 테스트식당 " + i)
                    .category(categories[i % categories.length])
                    .address("부산광역시 해운대구 엔플원로 " + i)
                    .description("N+1 테스트용 맛집")
                    .representativeMenu("테스트메뉴")
                    .businessHours("11:00~21:00")
                    .source(Source.MANUAL)
                    .member(member)
                    .build());
        }
        restaurantRepository.saveAll(restaurants);

        for (int i = 0; i < restaurants.size(); i++) {
            if (i % 5 == 0) {
                continue;   // 리뷰 없는 맛집 (평균 별점 null 처리도 같이 확인)
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
        System.out.println("=====================================================================================");
        System.out.printf(" 맛집 N+1 비교  (맛집 %d개, 리뷰 %d개, 페이지 크기 %d, 평균 %d회)%n",
                RESTAURANT_COUNT, reviews.size(), PAGE_SIZE, REPEAT);
        System.out.println("-------------------------------------------------------------------------------------");
        System.out.printf(" %-26s | %5s | %9s | %9s | %9s | %9s%n",
                "메서드", "결과수", "기존 쿼리", "변경 쿼리", "기존 ms", "변경 ms");
        System.out.println("-------------------------------------------------------------------------------------");
        resultLines.forEach(System.out::println);
        System.out.println("=====================================================================================");
        System.out.println(" ※ H2 메모리 DB 기준. 실제 MySQL 에서는 쿼리 1번마다 네트워크 왕복이 더해져 시간 차이가 커집니다.");
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────
    // 비교 대상 7개 (RestaurantService 의 .map(this::toResponse) 7곳)
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("① findAll() — 전체 목록")
    void findAll() {
        compare("findAll()",
                () -> restaurantService.findAll(),
                () -> toResponses(restaurantRepository.findAllByDeletedAtIsNull()));
    }

    @Test
    @DisplayName("② findByCategory(category) — 카테고리 목록")
    void findByCategory() {
        compare("findByCategory(KOREAN)",
                () -> restaurantService.findByCategory(Category.KOREAN),
                () -> toResponses(restaurantRepository.findByCategoryAndDeletedAtIsNull(Category.KOREAN)));
    }

    @Test
    @DisplayName("③ search(keyword) — 검색")
    void search() {
        compare("search(\"" + KEYWORD + "\")",
                () -> restaurantService.search(KEYWORD),
                () -> toResponses(restaurantRepository.searchByKeyword(KEYWORD)));
    }

    @Test
    @DisplayName("④ findLatest6() — 홈 최신 6개")
    void findLatest6() {
        compare("findLatest6()",
                () -> restaurantService.findLatest6(),
                () -> toResponses(restaurantRepository.findTop6ByDeletedAtIsNullOrderByCreatedAtDesc()));
    }

    @Test
    @DisplayName("⑤ findAll(pageable) — 전체 목록 페이징")
    void findAllPaged() {
        Pageable pageable = firstPage();
        compare("findAll(page)",
                () -> restaurantService.findAll(pageable).getContent(),
                () -> {
                    Page<Restaurant> page = restaurantRepository.findAllByDeletedAtIsNull(pageable);
                    return PageResponse.from(page, toResponses(page.getContent())).getContent();
                });
    }

    @Test
    @DisplayName("⑥ findByCategory(category, pageable) — 카테고리 페이징")
    void findByCategoryPaged() {
        Pageable pageable = firstPage();
        compare("findByCategory(KOREAN,page)",
                () -> restaurantService.findByCategory(Category.KOREAN, pageable).getContent(),
                () -> {
                    Page<Restaurant> page = restaurantRepository.findByCategoryAndDeletedAtIsNull(Category.KOREAN, pageable);
                    return PageResponse.from(page, toResponses(page.getContent())).getContent();
                });
    }

    @Test
    @DisplayName("⑦ search(keyword, pageable) — 검색 페이징")
    void searchPaged() {
        Pageable pageable = firstPage();
        compare("search(\"" + KEYWORD + "\",page)",
                () -> restaurantService.search(KEYWORD, pageable).getContent(),
                () -> {
                    Page<Restaurant> page = restaurantRepository.searchByKeyword(KEYWORD, pageable);
                    return PageResponse.from(page, toResponses(page.getContent())).getContent();
                });
    }

    // ─────────────────────────────────────────────────────────────
    // [변경 후 코드] RestaurantService 에 추가할 toResponses() 와 똑같은 내용
    // ─────────────────────────────────────────────────────────────
    private List<RestaurantResponse> toResponses(List<Restaurant> restaurants) {
        if (restaurants.isEmpty()) {
            return List.of();
        }

        List<Long> ids = restaurants.stream()
                .map(Restaurant::getId)
                .toList();

        Map<Long, Double> avgMap = reviewRepository.averageRatingsByRestaurantIdAvoidNPlusOne(ids).stream()
                .collect(Collectors.toMap(
                        ReviewRepository.RestaurantRating::getRestaurantId,
                        ReviewRepository.RestaurantRating::getAverageRating));

        return restaurants.stream()
                .map(r -> RestaurantResponse.from(r, avgMap.get(r.getId())))
                .toList();
    }

    // ─────────────────────────────────────────────────────────────
    // 측정 도구
    // ─────────────────────────────────────────────────────────────

    private Pageable firstPage() {
        // 테스트 데이터는 한 번에 저장해서 createdAt 이 같은 맛집이 많음
        //   → createdAt 만으로 정렬하면 실행할 때마다 순서가 바뀔 수 있어서 id 로 한 번 더 정렬
        return PageRequest.of(0, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    }

    /** 기존 / 변경 두 방식을 같은 조건으로 실행하고, 결과가 같은지 확인한 뒤 한 줄로 기록 */
    private void compare(String label,
                         Supplier<List<RestaurantResponse>> before,
                         Supplier<List<RestaurantResponse>> after) {

        Measurement oldResult = measure(before);
        Measurement newResult = measure(after);

        // 두 방식의 결과(순서, 맛집 id, 평균 별점)가 완전히 같아야 함
        assertThat(summary(newResult.result)).isEqualTo(summary(oldResult.result));
        assertThat(oldResult.result).isNotEmpty();
        // 변경 후: 맛집 조회 쿼리(+ 페이징이면 count 쿼리) + 평균 별점 IN 쿼리 1번 → 최대 3번
        assertThat(newResult.queries).isLessThanOrEqualTo(3);

        String line = String.format(" %-26s | %5d | %9d | %9d | %9.2f | %9.2f",
                label, oldResult.result.size(),
                oldResult.queries, newResult.queries,
                oldResult.avgMillis, newResult.avgMillis);
        resultLines.add(line);

        System.out.println("[N+1 비교] " + line.trim());
    }

    /** 새 읽기 전용 트랜잭션에서 실행 → 쿼리 수 1회 측정 + 시간 REPEAT 회 평균 */
    private Measurement measure(Supplier<List<RestaurantResponse>> action) {
        for (int i = 0; i < WARMUP; i++) {
            readOnlyTx.execute(status -> action.get());
        }

        // 쿼리 수 (한 번 실행했을 때 DB 로 보낸 SQL 수)
        statistics.clear();
        List<RestaurantResponse> result = readOnlyTx.execute(status -> action.get());
        long queries = statistics.getPrepareStatementCount();

        // 시간 (REPEAT 번 평균)
        long start = System.nanoTime();
        for (int i = 0; i < REPEAT; i++) {
            readOnlyTx.execute(status -> action.get());
        }
        double avgMillis = (System.nanoTime() - start) / 1_000_000.0 / REPEAT;

        return new Measurement(result, queries, avgMillis);
    }

    /** 비교용 요약: "id=평균별점" 목록 (순서 포함) */
    private List<String> summary(List<RestaurantResponse> list) {
        return list.stream()
                .map(r -> r.getId() + "=" + r.getAverageRating())
                .toList();
    }

    private record Measurement(List<RestaurantResponse> result, long queries, double avgMillis) {
    }
}
