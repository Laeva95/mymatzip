// ============================================================
// [MyPageApiController.java] 마이페이지 API 컨트롤러
// ============================================================
// 역할: 마이페이지에서 사용하는 데이터 API를 제공합니다.
//       내 맛집 목록, 내 리뷰 목록, 통계, 닉네임/비밀번호 변경 등
//       JavaScript의 fetch API로 호출하여 JSON 데이터를 주고받습니다.
//
//
// MVC 아키텍처에서의 위치:
//   ★Controller (요청 접수)★ → Service → Repository
//
// 사용되는 곳:
//   - mypage/dashboard.html에서 JavaScript fetch API로 호출
//   - 모든 API는 로그인한 사용자만 사용 가능 (@LoginMember)
// ============================================================

// ── 패키지 선언 ──────────────────────────────────────────────
package com.example.mymatzip.controller.api;

// ── import 문 ────────────────────────────────────────────────

// @LoginMember: 현재 로그인한 회원 정보를 컨트롤러 매개변수에 자동 주입하는 어노테이션
import com.example.mymatzip.config.security.CustomUserDetails;
import com.example.mymatzip.config.security.LoginMember;

// 엔티티와 DTO
import com.example.mymatzip.entity.Member;
import com.example.mymatzip.entity.Restaurant;
import com.example.mymatzip.entity.Review;

// 서비스 계층
import com.example.mymatzip.service.MemberService;
import com.example.mymatzip.service.RestaurantService;
import com.example.mymatzip.service.ReviewService;
import com.example.mymatzip.service.WishlistService;

// Repository (페이징 조회용)
import com.example.mymatzip.repository.RestaurantRepository;
import com.example.mymatzip.repository.ReviewRepository;
import com.example.mymatzip.repository.WishlistRepository;

// Lombok
import lombok.RequiredArgsConstructor;

// Spring 웹 관련
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

// Java 표준
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ── 클래스 선언 ──────────────────────────────────────────────

// @RestController: JSON 데이터를 반환하는 API 컨트롤러
@RestController

// @RequestMapping("/api/mypage"): 이 컨트롤러의 모든 API 경로 앞에 /api/mypage가 붙음
@RequestMapping("/api/mypage")

// @RequiredArgsConstructor: final 필드에 대한 생성자 자동 생성 (의존성 주입)
@RequiredArgsConstructor
public class MyPageApiController {

    // ── 의존성 필드 ─────────────────────────────────────────────
    // 마이페이지에서 필요한 서비스 및 리포지토리들
    private final MemberService memberService;
    private final RestaurantRepository restaurantRepository;
    private final ReviewRepository reviewRepository;
    private final WishlistRepository wishlistRepository;
    private final PasswordEncoder passwordEncoder;

    // ── 내 맛집 목록 조회 (페이징) ──────────────────────────────
    // URL: GET /api/mypage/restaurants?page=0&size=10
    // 동작: 로그인한 회원이 등록한 맛집 목록을 페이지별로 조회합니다.
    // @LoginMember: Spring Security에서 로그인한 회원 정보를 자동으로 가져옴
    @GetMapping("/restaurants")
    public ResponseEntity<Map<String, Object>> myRestaurants(
            @LoginMember Member loginMember,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // 페이징 요청 생성 (최신순 정렬)
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // DB에서 내가 등록한 맛집 조회
        Page<Restaurant> restaurantPage = restaurantRepository.findByMemberId(loginMember.getId(), pageable);

        // 날짜 포맷터: 2026-03-14 형식으로 변환
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 각 맛집을 Map으로 변환 (JSON 응답용)
        List<Map<String, Object>> content = restaurantPage.getContent().stream()
                .map(r -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", r.getId());
                    map.put("name", r.getName());
                    map.put("category", r.getCategory().getDisplayName());
                    map.put("address", r.getAddress());
                    map.put("createdAt", r.getCreatedAt().format(formatter));
                    return map;
                })
                .toList();

        // 응답 데이터 구성 (내용 + 페이지 정보)
        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("totalPages", restaurantPage.getTotalPages());
        response.put("totalElements", restaurantPage.getTotalElements());
        response.put("currentPage", restaurantPage.getNumber());

        return ResponseEntity.ok(response);
    }

    // ── 내 리뷰 목록 조회 (페이징) ──────────────────────────────
    // URL: GET /api/mypage/reviews?page=0&size=10
    // 동작: 로그인한 회원이 작성한 리뷰 목록을 페이지별로 조회합니다.
    @GetMapping("/reviews")
    public ResponseEntity<Map<String, Object>> myReviews(
            @LoginMember Member loginMember,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // 페이징 요청 생성 (최신순 정렬)
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // DB에서 내가 작성한 리뷰 조회
        Page<Review> reviewPage = reviewRepository.findByMemberId(loginMember.getId(), pageable);

        // 날짜 포맷터
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 각 리뷰를 Map으로 변환 (JSON 응답용)
        List<Map<String, Object>> content = reviewPage.getContent().stream()
                .map(r -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", r.getId());
                    map.put("content", r.getContent());
                    map.put("rating", r.getRating());
                    map.put("restaurantName", r.getRestaurant().getName());
                    map.put("restaurantId", r.getRestaurant().getId());
                    map.put("createdAt", r.getCreatedAt().format(formatter));
                    return map;
                })
                .toList();

        // 응답 데이터 구성
        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("totalPages", reviewPage.getTotalPages());
        response.put("totalElements", reviewPage.getTotalElements());
        response.put("currentPage", reviewPage.getNumber());

        return ResponseEntity.ok(response);
    }

    // ── 통계 조회 ───────────────────────────────────────────────
    // URL: GET /api/mypage/stats
    // 동작: 내 맛집 수, 내 리뷰 수, 내 찜 수를 한꺼번에 반환합니다.
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats(@LoginMember Member loginMember) {

        // 각 항목의 개수를 DB에서 조회
        long restaurantCount = restaurantRepository.countByMemberId(loginMember.getId());
        long reviewCount = reviewRepository.countByMemberId(loginMember.getId());
        long wishlistCount = wishlistRepository.countByMemberId(loginMember.getId());

        // 응답 데이터 구성
        Map<String, Object> response = new HashMap<>();
        response.put("restaurantCount", restaurantCount);
        response.put("reviewCount", reviewCount);
        response.put("wishlistCount", wishlistCount);

        return ResponseEntity.ok(response);
    }

    // ── 닉네임 변경 ─────────────────────────────────────────────
    // URL: PUT /api/mypage/nickname
    // 동작: 로그인한 회원의 닉네임을 변경합니다.
    // 요청 body 예시: {"nickname": "새닉네임"}
    @PutMapping("/nickname")
    public ResponseEntity<Map<String, Object>> updateNickname(
            @LoginMember Member loginMember,
            @RequestBody Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response) {

        String newNickname = body.get("nickname");

        // 닉네임 유효성 검사: 비어있으면 에러
        if (newNickname == null || newNickname.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "닉네임을 입력해주세요."));
        }

        try {
            // MemberService를 통해 닉네임 변경
            memberService.updateNickname(loginMember.getId(), newNickname.trim());
            refreshLoginMember(loginMember.getId(), request, response);  // [수정] 세션의 회원 정보도 새 값으로
            return ResponseEntity.ok(Map.of("message", "닉네임이 변경되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── 비밀번호 변경 ────────────────────────────────────────────
    // URL: PUT /api/mypage/password
    // 동작: 현재 비밀번호를 확인한 후 새 비밀번호로 변경합니다.
    // 요청 body 예시: {"currentPassword": "기존비번", "newPassword": "새비번"}
    @PutMapping("/password")
    public ResponseEntity<Map<String, Object>> updatePassword(
            @LoginMember Member loginMember,
            @RequestBody Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response) {

        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        // 유효성 검사: 비어있으면 에러
        if (currentPassword == null || newPassword == null
                || currentPassword.isEmpty() || newPassword.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "현재 비밀번호와 새 비밀번호를 모두 입력해주세요."));
        }

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, loginMember.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "현재 비밀번호가 일치하지 않습니다."));
        }

        try {
            // MemberService를 통해 비밀번호 변경
            memberService.updatePassword(loginMember.getId(), passwordEncoder.encode(newPassword));
            refreshLoginMember(loginMember.getId(), request, response);  // [수정] 세션의 회원 정보도 새 값으로
            return ResponseEntity.ok(Map.of("message", "비밀번호가 변경되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── [수정] 세션에 저장된 로그인 회원 정보 새로 고치기 ──────────────
    // 로그인할 때 세션에 넣어 둔 Member 는 그 순간의 값이라, 닉네임·비밀번호를 바꿔도
    // 다시 로그인하기 전까지 옛 값이 남습니다(메뉴의 닉네임, 다음 비밀번호 확인).
    // DB 에서 회원을 다시 읽어 로그인할 때와 같은 방법으로 인증 정보를 만들고 세션에 다시 저장합니다.
    // 세션이 없는 요청(JWT 토큰으로 호출한 API)은 저장할 세션이 없으므로 건너뜁니다.
    private void refreshLoginMember(Long memberId, HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) == null) {
            return;
        }
        CustomUserDetails userDetails = new CustomUserDetails(memberService.findById(memberId));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);
    }
}