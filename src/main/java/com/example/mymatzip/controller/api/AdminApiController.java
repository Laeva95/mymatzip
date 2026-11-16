// ============================================================
// [AdminApiController.java] 관리자 전용 API 컨트롤러
// ============================================================
// 역할: 관리자(ADMIN) 페이지에서 사용하는 데이터 API를 제공합니다.
//       대시보드 통계, 회원 관리, 맛집 관리, 리뷰 관리 등
//       JavaScript의 fetch API로 호출하여 JSON 데이터를 주고받습니다.
//
//
// ★ 접근 제한: Spring Security의 URL 규칙에 의해
//   ADMIN 역할을 가진 회원만 /api/admin/** 경로에 접근할 수 있습니다.
//
// 사용되는 곳:
//   - admin/dashboard.html, admin/members.html 등에서 fetch API로 호출
// ============================================================

// ── 패키지 선언 ──────────────────────────────────────────────
package com.example.mymatzip.controller.api;

// ── import 문 ────────────────────────────────────────────────

// @LoginMember: 로그인한 회원 정보를 자동 주입
import com.example.mymatzip.config.security.LoginMember;

// 엔티티
import com.example.mymatzip.entity.Member;
import com.example.mymatzip.entity.Restaurant;
import com.example.mymatzip.entity.Review;
import com.example.mymatzip.entity.Role;

// 서비스
import com.example.mymatzip.service.AdminService;

// Lombok
import lombok.RequiredArgsConstructor;

// Spring
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Java 표준
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ── 클래스 선언 ──────────────────────────────────────────────

// @RestController: JSON 데이터를 반환하는 API 컨트롤러
@RestController

// @RequestMapping("/api/admin"): 모든 API 경로 앞에 /api/admin이 붙음
@RequestMapping("/api/admin")

// @RequiredArgsConstructor: 의존성 주입을 위한 생성자 자동 생성
@RequiredArgsConstructor
public class AdminApiController {

    // ── 의존성 필드 ─────────────────────────────────────────────
    private final AdminService adminService;

    // ── 대시보드 통계 조회 ───────────────────────────────────────
    // URL: GET /api/admin/stats
    // 동작: 전체 회원 수, 맛집 수, 리뷰 수를 반환합니다.
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> stats(@LoginMember Member loginMember) {
        return ResponseEntity.ok(adminService.getStats());
    }

    // ── 전체 회원 목록 조회 (페이징) ────────────────────────────
    // URL: GET /api/admin/members?page=0&size=10
    // 동작: 모든 회원 목록을 페이지별로 조회합니다.
    @GetMapping("/members")
    public ResponseEntity<Map<String, Object>> members(
            @LoginMember Member loginMember,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Member> memberPage = adminService.findAllMembers(pageable);

        // 날짜 포맷터
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 각 회원을 Map으로 변환
        List<Map<String, Object>> content = memberPage.getContent().stream()
                .map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", m.getId());
                    map.put("email", m.getEmail());
                    map.put("nickname", m.getNickname());
                    map.put("role", m.getRole().name());
                    map.put("roleDisplayName", m.getRole().getDisplayName());
                    map.put("createdAt", m.getCreatedAt().format(formatter));
                    return map;
                })
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("totalPages", memberPage.getTotalPages());
        response.put("totalElements", memberPage.getTotalElements());
        response.put("currentPage", memberPage.getNumber());

        return ResponseEntity.ok(response);
    }

    // ── 회원 역할 변경 ──────────────────────────────────────────
    // URL: PUT /api/admin/members/{id}/role
    // 동작: 특정 회원의 역할(USER ↔ ADMIN)을 변경합니다.
    // 요청 body 예시: {"role": "ADMIN"}
    @PutMapping("/members/{id}/role")
    public ResponseEntity<Map<String, Object>> changeRole(
            @LoginMember Member loginMember,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        try {
            // 문자열을 Role enum으로 변환
            Role role = Role.valueOf(body.get("role"));
            adminService.changeRole(id, role);
            return ResponseEntity.ok(Map.of("message", "역할이 변경되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── 회원 삭제 ───────────────────────────────────────────────
    // URL: DELETE /api/admin/members/{id}
    // 동작: 특정 회원을 삭제합니다.
    @DeleteMapping("/members/{id}")
    public ResponseEntity<Map<String, Object>> deleteMember(
            @LoginMember Member loginMember,
            @PathVariable Long id) {
        try {
            adminService.deleteMember(id);
            return ResponseEntity.ok(Map.of("message", "회원이 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── 전체 맛집 목록 조회 (페이징) ────────────────────────────
    // URL: GET /api/admin/restaurants?page=0&size=10
    @GetMapping("/restaurants")
    public ResponseEntity<Map<String, Object>> restaurants(
            @LoginMember Member loginMember,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Restaurant> restaurantPage = adminService.findAllRestaurants(pageable);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<Map<String, Object>> content = restaurantPage.getContent().stream()
                .map(r -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", r.getId());
                    map.put("name", r.getName());
                    map.put("category", r.getCategory().getDisplayName());
                    map.put("address", r.getAddress());
                    map.put("memberNickname", r.getMember().getNickname());
                    map.put("createdAt", r.getCreatedAt().format(formatter));
                    return map;
                })
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("totalPages", restaurantPage.getTotalPages());
        response.put("totalElements", restaurantPage.getTotalElements());
        response.put("currentPage", restaurantPage.getNumber());

        return ResponseEntity.ok(response);
    }

    // ── 맛집 삭제 ───────────────────────────────────────────────
    // URL: DELETE /api/admin/restaurants/{id}
    @DeleteMapping("/restaurants/{id}")
    public ResponseEntity<Map<String, Object>> deleteRestaurant(
            @LoginMember Member loginMember,
            @PathVariable Long id) {
        try {
            adminService.deleteRestaurant(id);
            return ResponseEntity.ok(Map.of("message", "맛집이 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── 전체 리뷰 목록 조회 (신고 리뷰 관리용) ──────────────────
    // URL: GET /api/admin/reviews/reported
    // ★ 학습용: 실제로는 신고 기능과 연동하지만, 여기서는 전체 리뷰를 보여줍니다.
    @GetMapping("/reviews/reported")
    public ResponseEntity<Map<String, Object>> reportedReviews(
            @LoginMember Member loginMember,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Review> reviewPage = adminService.findReportedReviews(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("content", toReviewMaps(reviewPage.getContent()));
        response.put("totalPages", reviewPage.getTotalPages());
        response.put("totalElements", reviewPage.getTotalElements());
        response.put("currentPage", reviewPage.getNumber());

        return ResponseEntity.ok(response);
    }

    // ── [추가] 전체 리뷰 목록 ────────────────────────────────────
    // URL: GET /api/admin/reviews
    // 동작: 모든 리뷰를 신고 여부(reported)와 함께 반환합니다. (리뷰 관리 화면의 "전체" 버튼)
    @GetMapping("/reviews")
    public ResponseEntity<Map<String, Object>> allReviews(
            @LoginMember Member loginMember,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Review> reviewPage = adminService.findAllReviews(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("content", toReviewMaps(reviewPage.getContent()));
        response.put("totalPages", reviewPage.getTotalPages());
        response.put("totalElements", reviewPage.getTotalElements());
        response.put("currentPage", reviewPage.getNumber());

        return ResponseEntity.ok(response);
    }

    // 리뷰 엔티티 목록 → 화면용 Map 목록 (두 API 가 같은 형식을 쓰도록 한곳에 둠)
    private List<Map<String, Object>> toReviewMaps(List<Review> reviews) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return reviews.stream()
                .map(r -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", r.getId());
                    map.put("content", r.getContent());
                    map.put("rating", r.getRating());
                    map.put("memberNickname", r.getMember().getNickname());
                    map.put("restaurantName", r.getRestaurant().getName());
                    map.put("createdAt", r.getCreatedAt().format(formatter));
                    map.put("reported", r.isReported());
                    return map;
                })
                .toList();
    }

    // ── 리뷰 삭제 ───────────────────────────────────────────────
    // URL: DELETE /api/admin/reviews/{id}
    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Map<String, Object>> deleteReview(
            @LoginMember Member loginMember,
            @PathVariable Long id) {
        try {
            adminService.deleteReview(id);
            return ResponseEntity.ok(Map.of("message", "리뷰가 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}