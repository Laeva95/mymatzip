// ============================================================
// [WishlistRepository.java] 찜(위시리스트) 데이터 접근 인터페이스 (Repository)
// ============================================================
// 역할: 찜(Wishlist) 테이블에 대한 DB 접근(CRUD)을 담당합니다.
//       JpaRepository를 상속받으면 기본 CRUD 메서드가 자동으로 제공됩니다.
//
//
// ★ 핵심: Wishlist 테이블은 '누가(member_id) + 어떤 맛집을(restaurant_id) 찜했는지'
//   두 개의 외래키(FK)로 이루어진 관계 테이블입니다.
//
// [JpaRepository 기본 제공 메서드 — 아래에 안 보여도 사용 가능!]
//   - save(wishlist)   : 찜 등록 (하트 누르기)
//   - delete(wishlist) : 찜 해제 (하트 취소)
//   - findById(id)     : 찜 1건 조회
//   - count()          : 전체 찜 개수
//
// [Query Method에서 And 키워드]
//   조건 2개를 동시에 만족하는 데이터를 찾을 때 사용합니다.
//   findByMemberIdAndRestaurantId → WHERE member_id = ? AND restaurant_id = ?
//
// 사용되는 곳: WishlistService (찜 등록, 찜 해제, 찜 목록 조회, 찜 여부 확인)
// ============================================================

// --- 패키지 선언 ---
// 이 파일이 속한 폴더(패키지) 경로를 지정합니다.
// com.example.mymatzip.repository = "repository(저장소)" 계층에 해당
package com.example.mymatzip.repository;

// --- import 영역 (외부 클래스 가져오기) ---

// Wishlist 엔티티 가져오기 — 이 Repository가 다룰 대상(테이블)
// entity 패키지의 Wishlist 클래스 = DB의 wishlist 테이블과 매핑된 객체
// Wishlist는 "누가(member_id) 어떤 맛집을(restaurant_id) 찜했는지" 기록하는 테이블
import com.example.mymatzip.entity.Wishlist;

// JpaRepository 가져오기 — Spring Data JPA가 제공하는 "만능 창구" 인터페이스
// 이것을 상속(extends)하면 save, findById, findAll, delete 등이 자동 제공됨
import org.springframework.data.jpa.repository.JpaRepository;

// @EntityGraph 가져오기 — N+1 문제를 해결하는 어노테이션
import org.springframework.data.jpa.repository.EntityGraph;

// Page, Pageable 가져오기 — 페이징(한 페이지씩 나눠서 보기) 기능을 위한 클래스
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// List 가져오기 — 여러 개의 데이터를 담는 "목록" 자료구조
// 찜 목록은 여러 개가 조회될 수 있으므로 List<Wishlist>로 반환합니다.
import java.util.List;

// Optional 가져오기 — "값이 있을 수도, 없을 수도 있는 상자" 클래스
// null 대신 사용하여 NullPointerException(널 에러)을 방지하는 안전장치
import java.util.Optional;

// ★ interface(인터페이스) 선언
// 클래스(class)가 아니라 인터페이스(interface)인 이유:
//   → Spring Data JPA가 이 인터페이스를 보고 "구현체(실제 동작하는 코드)"를
//     런타임(실행 시점)에 자동으로 만들어줍니다.
//
// extends JpaRepository<Wishlist, Long> 의미:
//   → JpaRepository의 모든 기능을 물려받겠다는 선언
//   → <Wishlist, Long> = "Wishlist 엔티티를 다루며, 기본키(PK) 타입은 Long"
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    // ──────────────────────────────────────────────────────
    // 특정 회원의 찜 목록 조회 메서드
    // ──────────────────────────────────────────────────────
    // [사용 화면] 마이페이지 → "내가 찜한 맛집" 목록
    //
    // [매개변수]
    //   - Long memberId : 조회할 회원의 고유 번호(ID)
    //     예: 3L → 3번 회원이 찜한 맛집 목록을 가져옴
    //
    // [반환값]
    //   - List<Wishlist> : 해당 회원이 찜한 기록들의 목록
    //     → 찜한 맛집이 없으면 빈 리스트([])가 반환됩니다. (null이 아님!)
    //     → 각 Wishlist 객체 안에 restaurant 정보가 포함되어 있음
    //
    // [Query Method 해석]
    //   find + By + MemberId
    //   → SELECT * FROM wishlist WHERE member_id = ?
    //   → "wishlist 테이블에서 member_id가 일치하는 모든 행을 찾아라"
    //
    // [호출 예시 — WishlistService에서]
    //   List<Wishlist> myWishlist = wishlistRepository.findByMemberId(loginMemberId);
    // ──────────────────────────────────────────────────────
    // @EntityGraph: 찜 목록 조회 시 회원(member)과 맛집(restaurant) 정보를 JOIN으로 한꺼번에 가져옵니다.
    @EntityGraph(attributePaths = {"member", "restaurant"})
    List<Wishlist> findByMemberId(Long memberId);

    // ──────────────────────────────────────────────────────
    // [Phase 5] 특정 회원의 찜 목록 페이징 조회 메서드 (마이페이지용)
    // ──────────────────────────────────────────────────────
    // [사용 화면] 마이페이지 → "내가 찜한 맛집" 목록 (페이지별)
    //
    // [매개변수]
    //   - Long memberId : 조회할 회원의 고유 번호(ID)
    //   - Pageable pageable : 페이지 정보
    // [반환값]
    //   - Page<Wishlist> : 페이지 정보가 포함된 찜 목록
    // ──────────────────────────────────────────────────────
    @EntityGraph(attributePaths = {"restaurant"})
    Page<Wishlist> findByMemberId(Long memberId, Pageable pageable);

    // ──────────────────────────────────────────────────────
    // 특정 회원 + 특정 맛집의 찜 조회 메서드 (찜 취소 시 사용)
    // ──────────────────────────────────────────────────────
    // [사용 화면] 맛집 상세 페이지 → 하트(찜) 버튼을 다시 눌러 취소할 때
    //   1) 이 메서드로 해당 Wishlist 엔티티를 찾고
    //   2) delete(wishlist)로 삭제합니다.
    //
    // [매개변수]
    //   - Long memberId     : 찜을 취소하려는 회원의 고유 번호(ID)
    //   - Long restaurantId : 찜을 취소하려는 맛집의 고유 번호(ID)
    //
    // [반환값]
    //   - Optional<Wishlist> : 찜 기록이 담긴 "안전 상자"
    //     → 찜 기록이 있으면: Optional 안에 Wishlist 객체가 들어 있음
    //     → 찜 기록이 없으면: 빈 Optional (null 대신 안전하게 처리 가능)
    //
    // [Optional인 이유]
    //
    // [Query Method 해석]
    //   find + By + MemberId + And + RestaurantId
    //   → SELECT * FROM wishlist WHERE member_id = ? AND restaurant_id = ?
    //   → "회원 ID와 맛집 ID 두 조건을 동시에 만족하는 찜 기록을 찾아라"
    //
    // [호출 예시 — WishlistService에서]
    //   Optional<Wishlist> found = wishlistRepository
    //       .findByMemberIdAndRestaurantId(loginMemberId, restaurantId);
    //   found.ifPresent(wishlist -> wishlistRepository.delete(wishlist));
    // ──────────────────────────────────────────────────────
    Optional<Wishlist> findByMemberIdAndRestaurantId(Long memberId, Long restaurantId);

    // ──────────────────────────────────────────────────────
    // 이미 찜한 맛집인지 확인 메서드 (중복 찜 방지 및 하트 표시용)
    // ──────────────────────────────────────────────────────
    // [사용 화면] 맛집 상세 페이지 → 하트 아이콘을 빨간색(찜됨) / 빈 하트(미찜)로 표시할 때
    //            찜 버튼 클릭 시 → 이미 찜했으면 중복 등록을 방지
    //
    // [매개변수]
    //   - Long memberId     : 확인할 회원의 고유 번호(ID)
    //   - Long restaurantId : 확인할 맛집의 고유 번호(ID)
    //
    // [반환값]
    //   - boolean : true(이미 찜함 → 빨간 하트) / false(아직 안 찜함 → 빈 하트)
    //
    // [Query Method 해석]
    //   exists + By + MemberId + And + RestaurantId
    //   → 해당 조합이 DB에 존재하면 true, 없으면 false
    //   → 내부적으로: SELECT COUNT(*) > 0 FROM wishlist
    //                WHERE member_id = ? AND restaurant_id = ?
    //   → COUNT(*)가 1 이상이면 true(이미 찜함), 0이면 false(안 찜함)
    //
    // [호출 예시 — WishlistService에서]
    //   boolean isWished = wishlistRepository
    //       .existsByMemberIdAndRestaurantId(loginMemberId, restaurantId);
    //   // isWished가 true이면 빨간 하트, false이면 빈 하트를 프론트엔드에 전달
    // ──────────────────────────────────────────────────────
    boolean existsByMemberIdAndRestaurantId(Long memberId, Long restaurantId);

    // ──────────────────────────────────────────────────────
    // [Phase 9] 특정 회원이 찜한 맛집 수 조회 (마이페이지 통계용)
    // ──────────────────────────────────────────────────────
    // [사용 화면] 마이페이지 → 통계 카드에 "내 찜 수" 표시
    // [반환값] long — 해당 회원이 찜한 맛집의 총 개수
    // ──────────────────────────────────────────────────────
    long countByMemberId(Long memberId);
}