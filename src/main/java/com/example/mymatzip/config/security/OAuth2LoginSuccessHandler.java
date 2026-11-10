// ============================================================
// [OAuth2LoginSuccessHandler.java] OAuth2 소셜 로그인 성공 핸들러
// ============================================================
// 역할: 소셜 로그인(Google)이 성공한 후의 동작을 정의합니다.
//       로그인 성공 시 사용자를 홈페이지(/)로 리다이렉트(이동)시킵니다.
//
//
// 동작 흐름:
//   [소셜 로그인 성공] → [Spring Security가 이 핸들러 호출] → [홈으로 이동]
//
// 사용처: SecurityConfig에서 OAuth2 로그인 성공 핸들러로 등록됨
//   .oauth2Login()
//       .successHandler(oAuth2LoginSuccessHandler)
// ============================================================

// ── 패키지 선언 ──────────────────────────────────────────────
package com.example.mymatzip.config.security;

// ── import 문 ────────────────────────────────────────────────

// Spring Security 인증 관련
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

// Spring
import org.springframework.stereotype.Component;

// Servlet (HTTP 요청/응답 처리)
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Java 표준
import java.io.IOException;

// ── 클래스 선언 ──────────────────────────────────────────────

// @Component: Spring 빈으로 등록 — SecurityConfig에서 주입받아 사용
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    // ── onAuthenticationSuccess() 메서드 ─────────────────────────
    // Spring Security가 소셜 로그인 성공 시 자동으로 호출하는 메서드
    //
    // 매개변수:
    //   request — HTTP 요청 정보 (어떤 URL에서 왔는지 등)
    //   response — HTTP 응답 정보 (여기에 리다이렉트 응답을 설정)
    //   authentication — 인증 정보 (로그인한 사용자의 정보)
    //
    // 동작: 홈페이지(/)로 리다이렉트
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        // 로그인 성공 후 홈페이지(/)로 이동
        // sendRedirect: 브라우저에게 "이 주소로 다시 접속해"라고 응답하는 것
        response.sendRedirect("/");
    }
}