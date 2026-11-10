// ============================================================
// [AdminService.java] 관리자 전용 비즈니스 로직 서비스
// ============================================================
// 역할: 관리자(ADMIN) 전용 기능을 처리하는 서비스입니다.
//       전체 통계 조회, 회원 관리, 맛집 관리, 리뷰 관리 등
//       관리자 대시보드와 관리 페이지에서 사용하는 업무를 담당합니다.
//
//
// MVC 아키텍처에서의 위치:
//   Controller (요청 접수) → ★Service (업무 처리)★ → Repository (DB 접근)
//
// 사용처:
//   - AdminApiController에서 관리자 기능 API 호출 시 사용
//   - 관리자 대시보드, 회원 관리, 맛집 관리, 리뷰 관리 페이지
// ============================================================

// ── 패키지 선언 ──────────────────────────────────────────────
package com.example.mymatzip.service;

// ── import 문 ────────────────────────────────────────────────

// 엔티티 (DB 테이블과 매핑되는 클래스들)
import com.example.mymatzip.entity.Member;
import com.example.mymatzip.entity.Restaurant;
import com.example.mymatzip.entity.Review;
import com.example.mymatzip.entity.Role;

// Repository (DB 접근 인터페이스)
import com.example.mymatzip.repository.MemberRepository;
import com.example.mymatzip.repository.RestaurantRepository;
import com.example.mymatzip.repository.ReviewRepository;

// Lombok
import lombok.RequiredArgsConstructor;

// Spring
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Java 표준
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ── 클래스 선언 ──────────────────────────────────────────────

// @Service: 이 클래스가 비즈니스 로직을 담당하는 서비스임을 Spring에 등록
@Service

// @RequiredArgsConstructor: final 필드에 대한 생성자를 Lombok이 자동 생성 → DI(의존성 주입)
@RequiredArgsConstructor

// @Transactional(readOnly = true): 기본적으로 읽기 전용 트랜잭션 적용 (성능 최적화)
//   데이터를 변경하는 메서드에는 별도로 @Transactional을 붙여 쓰기 가능으로 전환
@Transactional(readOnly = true)
public class AdminService {

    // ── 의존성 필드 ─────────────────────────────────────────────
    // final: 한 번 주입된 후 변경 불가

    // memberRepository: 회원 테이블에 접근하는 저장소
    private final MemberRepository memberRepository;

    // restaurantRepository: 맛집 테이블에 접근하는 저장소
    private final RestaurantRepository restaurantRepository;

    // reviewRepository: 리뷰 테이블에 접근하는 저장소
    private final ReviewRepository reviewRepository;

    // ── 대시보드 통계 조회 ───────────────────────────────────────
    // 동작: 전체 회원 수, 맛집 수, 리뷰 수를 한꺼번에 조회하여 Map으로 반환
    // 사용처: 관리자 대시보드의 통계 카드에 표시
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalMembers", memberRepository.count());         // 전체 회원 수
        stats.put("totalRestaurants", restaurantRepository.count()); // 전체 맛집 수
        stats.put("totalReviews", reviewRepository.count());         // 전체 리뷰 수
        return stats;
    }

    // ── 전체 회원 목록 조회 (페이징) ────────────────────────────
    // 동작: 모든 회원 목록을 페이지별로 조회
    // 매개변수: Pageable — 페이지 번호와 크기 정보
    // 반환값: Page<Member> — 페이지 정보가 포함된 회원 목록
    // 사용처: 관리자 > 회원 관리 페이지
    public Page<Member> findAllMembers(Pageable pageable) {
        return memberRepository.findAll(pageable);
    }

    // ── 전체 맛집 목록 조회 (페이징) ────────────────────────────
    // 동작: 모든 맛집 목록을 페이지별로 조회
    // 사용처: 관리자 > 맛집 관리 페이지
    public Page<Restaurant> findAllRestaurants(Pageable pageable) {
        return restaurantRepository.findAll(pageable);
    }

    // ── 전체 리뷰 목록 조회 ──────────────────────────────────────
    // 동작: 모든 리뷰를 조회 (신고된 리뷰 관리용)
    // 반환값: List<Review> — 전체 리뷰 목록
    // 사용처: 관리자 > 리뷰 관리 페이지
    // ★ 실제 서비스에서는 신고(report) 기능과 연동하지만,
    //   학습용 프로젝트이므로 전체 리뷰를 보여줍니다.
    public List<Review> findAllReviews() {
        return reviewRepository.findAll();
    }

    // ── [추가] 신고된 리뷰만 조회 ────────────────────────────────
    // 반환값: List<Review> — reported = true 인 리뷰 목록
    // 사용처: 관리자 > 리뷰 관리 > "신고된 리뷰" 버튼 (GET /api/admin/reviews/reported)
    public List<Review> findReportedReviews() {
        return reviewRepository.findByReportedTrue();
    }

    // ── 회원 역할 변경 ──────────────────────────────────────────
    // 동작: 특정 회원의 역할(Role)을 변경합니다 (USER ↔ ADMIN)
    // 매개변수: memberId — 변경할 회원의 ID, role — 새로운 역할
    // @Transactional: DB에 데이터를 "수정"하므로 쓰기 가능 트랜잭션 필요
    @Transactional
    public void changeRole(Long memberId, Role role) {
        // 회원 조회 — 없으면 예외 발생
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 역할 변경 — Member 엔티티에 changeRole 메서드가 없으므로
        // JPA의 변경 감지(Dirty Checking)를 위해 직접 접근
        // ★ 다른 에이전트가 Member 엔티티에 changeRole 메서드를 추가할 예정
        member.changeRole(role);
    }

    // ── 맛집 삭제 ───────────────────────────────────────────────
    // 동작: 특정 맛집을 DB에서 삭제합니다.
    // 매개변수: id — 삭제할 맛집의 고유번호
    // @Transactional: DB에서 데이터를 "삭제"하므로 쓰기 가능 트랜잭션 필요
    @Transactional
    public void deleteRestaurant(Long id) {
        // 맛집 존재 확인 — 없으면 예외 발생
        if (!restaurantRepository.existsById(id)) {
            throw new IllegalArgumentException("맛집을 찾을 수 없습니다.");
        }
        restaurantRepository.deleteById(id);
    }

    // ── 리뷰 삭제 ───────────────────────────────────────────────
    // 동작: 특정 리뷰를 DB에서 삭제합니다.
    // 매개변수: id — 삭제할 리뷰의 고유번호
    @Transactional
    public void deleteReview(Long id) {
        // 리뷰 존재 확인 — 없으면 예외 발생
        if (!reviewRepository.existsById(id)) {
            throw new IllegalArgumentException("리뷰를 찾을 수 없습니다.");
        }
        reviewRepository.deleteById(id);
    }

    // ── 회원 삭제 ───────────────────────────────────────────────
    // 동작: 특정 회원을 DB에서 삭제합니다.
    // 매개변수: id — 삭제할 회원의 고유번호
    // ★ 주의: 실제 서비스에서는 관련 데이터(맛집, 리뷰, 찜) 처리가 필요합니다.
    @Transactional
    public void deleteMember(Long id) {
        // 회원 존재 확인 — 없으면 예외 발생
        if (!memberRepository.existsById(id)) {
            throw new IllegalArgumentException("회원을 찾을 수 없습니다.");
        }
        memberRepository.deleteById(id);
    }
}