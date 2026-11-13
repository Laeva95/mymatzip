// ============================================================
// [ReviewRepository.java] 리뷰 데이터 접근 인터페이스 (Repository)
// ============================================================
// 역할: 리뷰(Review) 테이블에 대한 DB 접근(CRUD)을 담당합니다.
//       JpaRepository를 상속받으면 기본 CRUD 메서드가 자동으로 제공됩니다.
//
//
// ★ 핵심: 간단한 조회는 메서드 이름으로, 평균 계산 같은 복잡한 작업은 @Query로 작성합니다.
//
// [JpaRepository 기본 제공 메서드 — 아래에 안 보여도 사용 가능!]
//   - save(review)    : 리뷰 저장(등록)
//   - findById(id)    : 리뷰 1개 조회
//   - findAll()       : 모든 리뷰 목록 조회
//   - deleteById(id)  : 리뷰 삭제
//   - count()         : 전체 리뷰 수
//
// 사용되는 곳: ReviewService (리뷰 등록, 리뷰 목록 조회, 평균 별점 계산 등)
// ============================================================

// --- 패키지 선언 ---
// 이 파일이 속한 폴더(패키지) 경로를 지정합니다.
// com.example.mymatzip.repository = "repository(저장소)" 계층에 해당
package com.example.mymatzip.repository;

// --- import 영역 (외부 클래스 가져오기) ---

// Review 엔티티 가져오기 — 이 Repository가 다룰 대상(테이블)
// entity 패키지의 Review 클래스 = DB의 review 테이블과 매핑된 객체
import com.example.mymatzip.entity.Review;

// JpaRepository 가져오기 — Spring Data JPA가 제공하는 "만능 창구" 인터페이스
// 이것을 상속(extends)하면 save, findById, findAll, delete 등이 자동 제공됨
import org.springframework.data.jpa.repository.JpaRepository;

// @Query 어노테이션 가져오기 — 메서드 이름 규칙으로 표현하기 어려운 복잡한 쿼리를
// JPQL(Java Persistence Query Language)로 직접 작성할 때 사용합니다.
// 여기서는 평균 별점 계산(AVG)에 사용됩니다.
import org.springframework.data.jpa.repository.Query;

// @Param 어노테이션 가져오기 — @Query 안의 :변수명과 메서드 파라미터를 연결할 때 사용
import org.springframework.data.repository.query.Param;

// @EntityGraph 가져오기 — N+1 문제를 해결하는 어노테이션
// 이 어노테이션이 없으면 리뷰 10개 조회 시 작성자 조회 쿼리가 10번 추가 실행되는 N+1 문제 발생!
import org.springframework.data.jpa.repository.EntityGraph;

// Page, Pageable 가져오기 — 페이징(한 페이지씩 나눠서 보기) 기능을 위한 클래스
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// List 가져오기 — 여러 개의 데이터를 담는 "목록" 자료구조
// 리뷰는 여러 개가 조회될 수 있으므로 List<Review>로 반환합니다.
import java.util.List;

// ★ interface(인터페이스) 선언
// 클래스(class)가 아니라 인터페이스(interface)인 이유:
//   → Spring Data JPA가 이 인터페이스를 보고 "구현체(실제 동작하는 코드)"를
//     런타임(실행 시점)에 자동으로 만들어줍니다.
//
// extends JpaRepository<Review, Long> 의미:
//   → JpaRepository의 모든 기능을 물려받겠다는 선언
//   → <Review, Long> = "Review 엔티티를 다루며, 기본키(PK) 타입은 Long"
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // ──────────────────────────────────────────────────────
    // 특정 맛집의 리뷰 목록 조회 메서드 (최신순 정렬)
    // ──────────────────────────────────────────────────────
    // [사용 화면] 맛집 상세 페이지 → 아래쪽 리뷰 목록 영역
    //
    // [매개변수]
    //   - Long restaurantId : 조회할 맛집의 고유 번호(ID)
    //     예: 5L → 5번 맛집의 리뷰들을 가져옴
    //
    // [반환값]
    //   - List<Review> : 해당 맛집에 달린 리뷰들의 목록 (최신순)
    //     → 리뷰가 없으면 빈 리스트([])가 반환됩니다.
    //
    // [Query Method 해석 — 이름을 쪼개서 읽기]
    //   find              → 조회해라
    //   By                → 조건 시작
    //   RestaurantId      → restaurant 엔티티의 id가 일치하는 것만
    //   OrderBy           → 정렬 기준 시작
    //   CreatedAt         → createdAt 필드(작성일시) 기준
    //   Desc              → 내림차순 (최신이 위로)
    //
    //   최종 SQL: SELECT * FROM review WHERE restaurant_id = ? ORDER BY created_at DESC
    //
    // [호출 예시 — ReviewService에서]
    //   List<Review> reviews = reviewRepository.findByRestaurantIdOrderByCreatedAtDesc(5L);
    // ──────────────────────────────────────────────────────
    // @EntityGraph: 리뷰 조회 시 작성자(member) 정보를 JOIN으로 한꺼번에 가져옵니다.
    // 이 어노테이션이 없으면 N+1 문제 발생 — 리뷰 10개 조회 시 작성자 조회 쿼리가 10번 추가 실행!
    @EntityGraph(attributePaths = {"member"})
    List<Review> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);

    // ──────────────────────────────────────────────────────
    // 특정 맛집의 평균 별점 계산 메서드
    // ──────────────────────────────────────────────────────
    // [사용 화면] 맛집 상세 페이지 → "평균 ★ 4.3" 표시 영역
    //            맛집 목록 페이지 → 각 맛집 카드에 평균 별점 표시
    //
    // [매개변수]
    //   - Long restaurantId : 평균 별점을 구할 맛집의 고유 번호(ID)
    //
    // [반환값]
    //   - Double : 해당 맛집의 평균 별점 (소수점 포함)
    //     → 예: 4.3, 3.7, 5.0 등
    //     → 리뷰가 하나도 없으면 null이 반환됩니다.
    //
    // [@Query로 직접 작성한 이유]
    //   "평균 계산(AVG)"은 메서드 이름 규칙만으로 표현할 수 없어서
    //   JPQL로 직접 작성합니다.
    //
    // [JPQL 한 줄씩 해석]
    //   SELECT AVG(r.rating)                  → rating 필드의 평균값을 구해라
    //   FROM Review r                         → Review 엔티티에서 r이라는 별명으로
    //   WHERE r.restaurant.id = :restaurantId → 해당 맛집의 리뷰만 대상으로
    //
    // [반환 타입이 Double(대문자)인 이유]
    //
    // [호출 예시 — RestaurantService에서]
    //   Double avg = reviewRepository.averageRatingByRestaurantId(5L);
    //   double displayRating = (avg != null) ? avg : 0.0;  // null이면 0.0으로 처리
    // ──────────────────────────────────────────────────────
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.restaurant.id = :restaurantId")  // AVG()로 평균 계산, :restaurantId는 아래 @Param으로 연결
    Double averageRatingByRestaurantId(@Param("restaurantId") Long restaurantId);        // @Param으로 JPQL의 :restaurantId에 실제 값을 바인딩

    // ──────────────────────────────────────────────────────
    // 특정 회원이 작성한 리뷰 목록 조회 메서드
    // ──────────────────────────────────────────────────────
    // [사용 화면] 마이페이지 → "내가 쓴 리뷰" 목록
    //
    // [매개변수]
    //   - Long memberId : 조회할 회원의 고유 번호(ID)
    //     예: 3L → 3번 회원이 작성한 리뷰들을 가져옴
    //
    // [반환값]
    //   - List<Review> : 해당 회원이 작성한 리뷰들의 목록
    //     → 작성한 리뷰가 없으면 빈 리스트([])가 반환됩니다.
    //
    // [Query Method 해석]
    //   find + By + MemberId
    //   → SELECT * FROM review WHERE member_id = ?
    //   → "review 테이블에서 member_id가 일치하는 모든 행을 찾아라"
    //
    // [호출 예시 — ReviewService에서]
    //   List<Review> myReviews = reviewRepository.findByMemberId(loginMemberId);
    // ──────────────────────────────────────────────────────
    List<Review> findByMemberId(Long memberId);

    // ──────────────────────────────────────────────────────
    // [Phase 5] 맛집별 리뷰 페이징 조회 메서드
    // ──────────────────────────────────────────────────────
    // 위의 findByRestaurantIdOrderByCreatedAtDesc()와 같은 역할이지만, 페이징을 지원합니다.
    //
    // [매개변수]
    //   - Long restaurantId : 리뷰를 조회할 맛집의 고유번호(ID)
    //   - Pageable pageable : 페이지 번호와 크기 정보
    // [반환값]
    //   - Page<Review> : 페이지 정보가 포함된 리뷰 목록
    // ──────────────────────────────────────────────────────
    @EntityGraph(attributePaths = {"member"})
    Page<Review> findByRestaurantId(Long restaurantId, Pageable pageable);

    // ──────────────────────────────────────────────────────
    // [Phase 5] 특정 회원의 리뷰 페이징 조회 메서드 (마이페이지용)
    // ──────────────────────────────────────────────────────
    // [사용 화면] 마이페이지 → "내가 쓴 리뷰" 목록 (페이지별)
    //
    // @EntityGraph에 "restaurant"도 포함하는 이유:
    //   마이페이지에서는 "어떤 맛집에 쓴 리뷰인지"도 함께 보여줘야 하므로,
    //   작성자(member) + 맛집(restaurant) 정보를 한꺼번에 가져옵니다.
    //
    // [매개변수]
    //   - Long memberId : 조회할 회원의 고유 번호(ID)
    //   - Pageable pageable : 페이지 정보
    // [반환값]
    //   - Page<Review> : 페이지 정보가 포함된 리뷰 목록
    // ──────────────────────────────────────────────────────
    @EntityGraph(attributePaths = {"member", "restaurant"})
    Page<Review> findByMemberId(Long memberId, Pageable pageable);

    // ──────────────────────────────────────────────────────
    // [Phase 10] 신고된 리뷰 목록 조회 메서드 (관리자용)
    // ──────────────────────────────────────────────────────
    // [사용 화면] 관리자 페이지 → "신고된 리뷰" 목록
    //
    // [반환값]
    //   - List<Review> : 신고된(reported=true) 리뷰 목록
    // ──────────────────────────────────────────────────────
    List<Review> findByReportedTrue();
    // ──────────────────────────────────────────────────────
    // [Phase 10] 신고된 리뷰 페이징 조회 메서드 (관리자용)
    // ──────────────────────────────────────────────────────
    // [사용 화면] 관리자 페이지 → "신고된 리뷰" 목록 (페이지별)
    //
    // [매개변수]
    //   - Pageable pageable : 페이지 번호와 페이지 크기 정보
    //
    // [반환값]
    //   - Page<Review> : 페이지 정보가 포함된 신고된 리뷰 목록
    // ──────────────────────────────────────────────────────
    Page<Review> findByReportedTrue(Pageable pageable);

    // ──────────────────────────────────────────────────────
    // [Phase 9] 특정 회원이 작성한 리뷰 수 조회 (마이페이지 통계용)
    // ──────────────────────────────────────────────────────
    // [사용 화면] 마이페이지 → 통계 카드에 "내 리뷰 수" 표시
    // [반환값] long — 해당 회원이 작성한 리뷰의 총 개수
    // ──────────────────────────────────────────────────────
    long countByMemberId(Long memberId);
}