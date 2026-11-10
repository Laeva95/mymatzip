// ============================================================
// [RestaurantRepository.java] 맛집 데이터 접근 인터페이스 (Repository)
// ============================================================
// 역할: 맛집(Restaurant) 테이블에 대한 DB 접근(CRUD)을 담당합니다.
//       JpaRepository를 상속받으면 기본 CRUD 메서드가 자동으로 제공됩니다.
//
//
// ★ 핵심: 메서드 이름을 규칙대로 지으면 Spring이 자동으로 SQL을 만들어 주고,
//   복잡한 검색은 @Query로 직접 작성할 수도 있습니다.
//
// [JpaRepository 기본 제공 메서드 — 아래에 안 보여도 사용 가능!]
//   - save(restaurant)   : 맛집 저장(INSERT) 또는 수정(UPDATE)
//   - findById(id)       : ID로 맛집 1개 조회 (상세 페이지)
//   - findAll()          : 모든 맛집 목록 조회
//   - deleteById(id)     : 맛집 삭제
//   - count()            : 전체 맛집 수
//
// [아래 메서드에서 사용하는 두 가지 쿼리 방식]
//   1) Query Method : 메서드 이름 규칙으로 자동 생성 (findByCategory 등)
//   2) @Query(JPQL)  : 복잡한 쿼리를 직접 작성 (searchByKeyword)
//
// 사용되는 곳: RestaurantService (맛집 등록, 목록 조회, 검색 등)
// ============================================================

// --- 패키지 선언 ---
// 이 파일이 속한 폴더(패키지) 경로를 지정합니다.
// com.example.mymatzip.repository = "repository(저장소)" 계층에 해당
package com.example.mymatzip.repository;

// --- import 영역 (외부 클래스 가져오기) ---

// Category 열거형(enum) 가져오기 — 맛집의 음식 종류(한식, 중식, 일식 등)를 정의한 enum
// findByCategory() 메서드의 파라미터 타입으로 사용됩니다.
import com.example.mymatzip.entity.Category;

// Restaurant 엔티티 가져오기 — 이 Repository가 다룰 대상(테이블)
// entity 패키지의 Restaurant 클래스 = DB의 restaurant 테이블과 매핑된 객체
import com.example.mymatzip.entity.Restaurant;

// JpaRepository 가져오기 — Spring Data JPA가 제공하는 "만능 창구" 인터페이스
// 이것을 상속(extends)하면 save, findById, findAll, delete 등이 자동 제공됨
import org.springframework.data.jpa.repository.JpaRepository;

// @EntityGraph 가져오기 — N+1 문제를 해결하는 어노테이션
// N+1 문제란? 맛집 10개를 조회하면 등록자 정보를 가져오기 위해 추가로 10번 DB를 조회하는 비효율
// @EntityGraph를 붙이면 맛집 + 등록자를 한 번의 쿼리(JOIN)로 가져옵니다.
import org.springframework.data.jpa.repository.EntityGraph;

// @Query 어노테이션 가져오기 — 메서드 이름 규칙으로 표현하기 어려운 복잡한 쿼리를
// JPQL(Java Persistence Query Language)로 직접 작성할 때 사용합니다.
import org.springframework.data.jpa.repository.Query;

// Page, Pageable 가져오기 — 페이징(한 페이지씩 나눠서 보기) 기능을 위한 클래스
// Pageable = "몇 번째 페이지, 한 페이지에 몇 개" 요청 정보
// Page = "현재 페이지 데이터 + 전체 페이지 수 + 전체 데이터 수" 응답 정보
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// @Param 어노테이션 가져오기 — @Query 안의 :변수명과 메서드 파라미터를 연결할 때 사용
import org.springframework.data.repository.query.Param;

// List 가져오기 — 여러 개의 데이터를 담는 "목록" 자료구조
// 맛집은 여러 개가 조회될 수 있으므로 List<Restaurant>로 반환합니다.
import java.util.List;

// ★ interface(인터페이스) 선언
// 클래스(class)가 아니라 인터페이스(interface)인 이유:
//   → Spring Data JPA가 이 인터페이스를 보고 "구현체(실제 동작하는 코드)"를
//     런타임(실행 시점)에 자동으로 만들어줍니다.
//
// extends JpaRepository<Restaurant, Long> 의미:
//   → JpaRepository의 모든 기능을 물려받겠다는 선언
//   → <Restaurant, Long> = "Restaurant 엔티티를 다루며, 기본키(PK) 타입은 Long"
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    // ──────────────────────────────────────────────────────
    // 카테고리별 맛집 목록 조회 메서드
    // ──────────────────────────────────────────────────────
    // [사용 화면] 맛집 목록 페이지 → "한식 / 중식 / 일식 ..." 탭 클릭 시
    //
    // [매개변수]
    //   - Category category : 음식 카테고리 enum 값
    //     예: Category.KOREAN(한식), Category.CHINESE(중식), Category.JAPANESE(일식)
    //
    // [반환값]
    //   - List<Restaurant> : 해당 카테고리에 속하는 맛집들의 목록(리스트)
    //     → 결과가 0개이면 빈 리스트([])가 반환됩니다. (null이 아님!)
    //
    // [Query Method 해석 — 메서드 이름이 SQL로 변환됨]
    //   find + By + Category
    //   → SELECT * FROM restaurant WHERE category = ?
    //   → "restaurant 테이블에서 category 컬럼이 일치하는 모든 행을 찾아라"
    //   Category는 enum(열거형)이지만 사용법은 문자열 검색과 동일합니다.
    //
    // [호출 예시 — RestaurantService에서]
    //   List<Restaurant> koreanList = restaurantRepository.findByCategory(Category.KOREAN);
    // ──────────────────────────────────────────────────────
    // @EntityGraph: 맛집 조회 시 등록자(member) 정보를 JOIN으로 한꺼번에 가져옵니다.
    // 이 어노테이션이 없으면 N+1 문제 발생 — 맛집 10개 조회 시 등록자 조회 쿼리가 10번 추가 실행!
    @EntityGraph(attributePaths = {"member"})
    List<Restaurant> findByCategory(Category category);

    // ──────────────────────────────────────────────────────
    // [Phase 5] 카테고리별 맛집 목록 페이징 조회 메서드
    // ──────────────────────────────────────────────────────
    // 위의 findByCategory()와 같은 기능이지만, 페이징을 지원합니다.
    //
    // [매개변수]
    //   - Category category : 조회할 카테고리 (한식, 중식, 양식 등)
    //   - Pageable pageable : 페이지 번호와 크기 정보 (예: 0번째 페이지, 10개씩)
    // [반환값]
    //   - Page<Restaurant> : 페이지 정보(전체 개수, 총 페이지 수 등)가 포함된 결과
    // ──────────────────────────────────────────────────────
    @EntityGraph(attributePaths = {"member"})
    Page<Restaurant> findByCategory(Category category, Pageable pageable);

    // ──────────────────────────────────────────────────────
    // 키워드로 맛집 검색 메서드 (이름 또는 주소에 포함)
    // ──────────────────────────────────────────────────────
    // [사용 화면] 검색창에 "강남 돈까스" 같은 키워드를 입력했을 때
    //
    // [매개변수]
    //   - String keyword : 사용자가 검색창에 입력한 검색어
    //     예: "돈까스", "강남", "분식"
    //
    // [반환값]
    //   - List<Restaurant> : 이름 또는 주소에 키워드가 포함된 맛집 목록
    //     → 결과가 0개이면 빈 리스트([])가 반환됩니다.
    //
    // [@Query란?]
    //   메서드 이름만으로는 표현하기 어려운 복잡한 쿼리를 직접 작성하는 방법입니다.
    //
    // [JPQL 한 줄씩 해석]
    //   SELECT r FROM Restaurant r           → Restaurant 엔티티에서 r이라는 별명으로 조회
    //   WHERE LOWER(r.name) LIKE ...         → 가게 이름을 소문자로 바꾼 후 키워드가 포함되는지 확인
    //   OR LOWER(r.address) LIKE ...         → 또는 주소를 소문자로 바꾼 후 키워드가 포함되는지 확인
    //   LOWER()는 대소문자를 무시하고 검색하기 위한 함수입니다.
    //   CONCAT('%', :keyword, '%')는 "앞뒤 어디든 포함"이라는 의미입니다.
    //     예: '%돈까스%' → "돈까스"가 어디든 포함된 것을 찾음
    //
    // [@Param이란?]
    //   JPQL 속의 :keyword 자리에 실제 파라미터 값을 넣어주는 연결 고리입니다.
    //
    // [호출 예시 — RestaurantService에서]
    //   List<Restaurant> results = restaurantRepository.searchByKeyword("돈까스");
    // ──────────────────────────────────────────────────────
    @EntityGraph(attributePaths = {"member"})
    @Query("SELECT r FROM Restaurant r " +                                          // Restaurant 엔티티에서 r이라는 별명으로 조회
           "WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +          // 이름에 키워드가 포함되거나 (대소문자 무시)
           "OR LOWER(r.address) LIKE LOWER(CONCAT('%', :keyword, '%'))")            // 주소에 키워드가 포함된 것을 찾음 (대소문자 무시)
    List<Restaurant> searchByKeyword(@Param("keyword") String keyword);             // @Param으로 :keyword에 실제 값을 바인딩

    // ──────────────────────────────────────────────────────
    // [Phase 5] 키워드 검색 + 페이징 메서드
    // ──────────────────────────────────────────────────────
    // 위의 searchByKeyword()와 같은 JPQL이지만, 페이징을 지원합니다.
    // Pageable을 파라미터로 받으면 Spring Data JPA가 자동으로
    // LIMIT/OFFSET을 붙여 해당 페이지의 데이터만 가져옵니다.
    //
    // [반환값]
    //   - Page<Restaurant> : 페이지 정보 + 검색 결과 데이터
    // ──────────────────────────────────────────────────────
    @EntityGraph(attributePaths = {"member"})
    @Query("SELECT r FROM Restaurant r " +
           "WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(r.address) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Restaurant> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // ──────────────────────────────────────────────────────
    // 최신 등록순으로 상위 6개 맛집 조회 메서드
    // ──────────────────────────────────────────────────────
    // [사용 화면] 메인(홈) 페이지 → "최근 등록된 맛집" 섹션
    //
    // [매개변수] 없음 — 조건 없이 전체 대상에서 상위 6개를 가져옵니다.
    //
    // [반환값]
    //   - List<Restaurant> : 최신순으로 정렬된 맛집 6개의 목록
    //     → 맛집이 6개 미만이면 있는 만큼만 반환됩니다.
    //
    // [Query Method 해석 — 이름이 길지만 규칙대로 쪼개면 간단!]
    //   find           → 조회해라
    //   Top6           → 상위 6개만 (SQL의 LIMIT 6에 해당)
    //   By             → 조건 시작 (여기서는 조건 없이 전체 대상)
    //   OrderBy        → 정렬 기준 시작
    //   CreatedAt      → createdAt 필드(등록일시) 기준
    //   Desc           → 내림차순 (최신이 위로, 가장 최근 것이 1번)
    //
    //   최종 SQL: SELECT * FROM restaurant ORDER BY created_at DESC LIMIT 6
    //
    // [호출 예시 — RestaurantService에서]
    //   List<Restaurant> latest6 = restaurantRepository.findTop6ByOrderByCreatedAtDesc();
    // ──────────────────────────────────────────────────────
    @EntityGraph(attributePaths = {"member"})
    List<Restaurant> findTop6ByOrderByCreatedAtDesc();

    // ──────────────────────────────────────────────────────
    // [Phase 5] 전체 맛집 목록 페이징 조회 메서드
    // ──────────────────────────────────────────────────────
    // JpaRepository가 기본 제공하는 findAll()을 오버라이드하여
    // @EntityGraph를 적용합니다. 등록자(member) 정보를 JOIN으로 한꺼번에 가져옵니다.
    //
    // [매개변수]
    //   - Pageable pageable : 페이지 번호와 크기 정보 (예: 0번째 페이지, 10개씩)
    // [반환값]
    //   - Page<Restaurant> : 페이지 정보가 포함된 맛집 목록
    // ──────────────────────────────────────────────────────
    @EntityGraph(attributePaths = {"member"})
    Page<Restaurant> findAll(Pageable pageable);

    // ──────────────────────────────────────────────────────
    // [Phase 5] 특정 회원이 등록한 맛집 목록 페이징 조회 (마이페이지용)
    // ──────────────────────────────────────────────────────
    // [사용 화면] 마이페이지 → "내가 등록한 맛집" 목록
    //
    // [매개변수]
    //   - Long memberId : 조회할 회원의 고유 번호(ID)
    //   - Pageable pageable : 페이지 정보
    // [반환값]
    //   - Page<Restaurant> : 해당 회원이 등록한 맛집 목록 (페이징 적용)
    // ──────────────────────────────────────────────────────
    @EntityGraph(attributePaths = {"member"})
    Page<Restaurant> findByMemberId(Long memberId, Pageable pageable);

    // ──────────────────────────────────────────────────────
    // [Phase 9] 특정 회원이 등록한 맛집 수 조회 (마이페이지 통계용)
    // ──────────────────────────────────────────────────────
    // [사용 화면] 마이페이지 → 통계 카드에 "내 맛집 수" 표시
    // [반환값] long — 해당 회원이 등록한 맛집의 총 개수
    // ──────────────────────────────────────────────────────
    long countByMemberId(Long memberId);
}
