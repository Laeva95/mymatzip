// ============================================================
// [AdminViewController.java] 관리자 페이지 화면 컨트롤러
// ============================================================
// 역할: 관리자(ADMIN) 전용 페이지의 화면(타임리프 화면)을 반환하는 컨트롤러입니다.
//       관리자 대시보드, 회원 관리, 맛집 관리, 리뷰 관리 화면을 제공합니다.
//
//
// URL 매핑:
//   - /admin          → 관리자 대시보드
//   - /admin/members  → 회원 관리
//   - /admin/restaurants → 맛집 관리
//   - /admin/reviews  → 리뷰 관리
//
// ★ 접근 제한: Spring Security의 URL 규칙에 의해
//   ADMIN 역할을 가진 회원만 /admin/** 경로에 접근할 수 있습니다.
// ============================================================

// ── 패키지 선언 ──────────────────────────────────────────────
package com.example.mymatzip.controller.view;

// ── import 문 ────────────────────────────────────────────────

// @Controller: 화면(타임리프 화면)을 반환하는 컨트롤러임을 Spring에 알림
import org.springframework.stereotype.Controller;

// @GetMapping: HTTP GET 요청을 처리하는 어노테이션
import org.springframework.web.bind.annotation.GetMapping;

// @RequestMapping: 이 컨트롤러의 기본 URL 경로를 설정
import org.springframework.web.bind.annotation.RequestMapping;

// ── 클래스 선언 ──────────────────────────────────────────────

// @Controller: Spring에게 "이 클래스는 화면(타임리프 화면)을 반환하는 컨트롤러입니다"라고 등록
@Controller

// @RequestMapping("/admin"): 이 컨트롤러의 모든 URL 앞에 /admin이 붙음
@RequestMapping("/admin")
public class AdminViewController {

    // ── 관리자 대시보드 ─────────────────────────────────────────
    // URL: GET /admin
    // 반환값: "admin/dashboard" → templates/admin/dashboard.html
    @GetMapping
    public String dashboard() {
        return "admin/dashboard";
    }

    // ── 회원 관리 페이지 ────────────────────────────────────────
    // URL: GET /admin/members
    // 반환값: "admin/members" → templates/admin/members.html
    @GetMapping("/members")
    public String members() {
        return "admin/members";
    }

    // ── 맛집 관리 페이지 ────────────────────────────────────────
    // URL: GET /admin/restaurants
    // 반환값: "admin/restaurants" → templates/admin/restaurants.html
    @GetMapping("/restaurants")
    public String restaurants() {
        return "admin/restaurants";
    }

    // ── 리뷰 관리 페이지 ────────────────────────────────────────
    // URL: GET /admin/reviews
    // 반환값: "admin/reviews" → templates/admin/reviews.html
    @GetMapping("/reviews")
    public String reviews() {
        return "admin/reviews";
    }
}