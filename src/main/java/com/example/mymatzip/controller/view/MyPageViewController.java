// ============================================================
// [MyPageViewController.java] 마이페이지 화면 컨트롤러
// ============================================================
// 역할: 마이페이지(내 정보, 내 맛집, 내 리뷰) 화면을 보여주는 컨트롤러입니다.
//       브라우저에서 /mypage 주소로 접속하면 대시보드 타임리프 화면를 반환합니다.
//
//
// MVC 아키텍처에서의 위치:
//   ★Controller (요청 접수)★ → Service → Repository
//
// 사용되는 곳:
//   - 로그인한 사용자가 "마이페이지" 메뉴를 클릭했을 때
//   - URL: http://localhost:8080/mypage
// ============================================================

// ── 패키지 선언 ──────────────────────────────────────────────
package com.example.mymatzip.controller.view;

// ── import 문 ────────────────────────────────────────────────

// @Controller: 이 클래스가 화면(View)을 반환하는 컨트롤러임을 Spring에 알림
// @RestController와 달리 타임리프 화면 이름을 반환합니다.
import org.springframework.stereotype.Controller;

// @GetMapping: HTTP GET 요청을 처리하는 어노테이션
// 브라우저에서 주소를 입력하거나 링크를 클릭하면 GET 요청이 발생합니다.
import org.springframework.web.bind.annotation.GetMapping;

// @RequiredArgsConstructor: Lombok이 final 필드에 대한 생성자를 자동 생성
import lombok.RequiredArgsConstructor;

// ── 클래스 선언 ──────────────────────────────────────────────

// @Controller: Spring에게 "이 클래스는 화면(타임리프 화면)을 반환하는 컨트롤러입니다"라고 등록
@Controller

// @RequiredArgsConstructor: 의존성 주입을 위한 생성자 자동 생성
@RequiredArgsConstructor
public class MyPageViewController {

    // ── 마이페이지 대시보드 화면 ────────────────────────────────
    // URL: GET /mypage
    // 동작: 마이페이지 대시보드 타임리프 화면을 반환합니다.
    // 반환값: "mypage/dashboard" → templates/mypage/dashboard.html
    @GetMapping("/mypage")
    public String dashboard() {
        return "mypage/dashboard";
    }
}