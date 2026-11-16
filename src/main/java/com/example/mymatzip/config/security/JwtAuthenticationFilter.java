/*
 * ============================================================================
 *  파일명    : JwtAuthenticationFilter.java
 *  위치      : com.example.mymatzip.config.security (보안 설정 패키지)
 *  역할      : API 요청(/api/**)에서 JWT 토큰을 검증하는 "API 전용 보안 검문소"
 * ============================================================================
 *
 *
 *  ■ OncePerRequestFilter란?
 *    "한 번의 요청에 한 번만 실행되는 필터"입니다.
 *    일반 Filter는 포워드(forward) 등으로 여러 번 실행될 수 있지만,
 *    OncePerRequestFilter는 같은 요청에 대해 딱 한 번만 실행됩니다.
 *
 *  ■ 동작 흐름
 *    1) HTTP 요청이 들어옴
 *    2) URL이 /api/**가 아니면 → 바로 통과 (세션 기반 인증 사용)
 *    3) URL이 /api/**이면 → Authorization 헤더에서 Bearer 토큰 추출
 *    4) 토큰이 유효하면 → SecurityContextHolder에 인증 정보 설정
 *    5) 토큰이 없거나 유효하지 않으면 → 인증 정보 설정 없이 통과
 *       (이후 SecurityConfig의 권한 설정에 의해 허용/거부 결정)
 *
 *  ■ MVC에서의 위치
 *    [요청] → [이 JWT 필터] → [Spring Security 필터들] → [Controller]
 *    SecurityConfig에서 UsernamePasswordAuthenticationFilter 앞에 배치됩니다.
 * ============================================================================
 */

// ── 패키지 선언 ──────────────────────────────────────────────
package com.example.mymatzip.config.security;

// ── import 문 ────────────────────────────────────────────────

// HttpServletRequest/Response: HTTP 요청/응답 객체
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// @RequiredArgsConstructor: final 필드에 대한 생성자 자동 생성
import lombok.RequiredArgsConstructor;

// MemberService: 회원 ID로 회원 정보를 조회하기 위한 서비스
import com.example.mymatzip.entity.Member;
import com.example.mymatzip.service.MemberService;

// Spring Security 관련 import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

// OncePerRequestFilter: 요청당 한 번만 실행되는 필터
import org.springframework.web.filter.OncePerRequestFilter;

// IOException: 입출력 예외
import java.io.IOException;

// ── @Component ───────────────────────────────────────────────
// 스프링 빈으로 등록 → SecurityConfig에서 주입받아 필터 체인에 추가
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // ── 필드 (의존성) ────────────────────────────────────────

    // JwtTokenProvider: JWT 토큰을 생성/검증/파싱하는 유틸리티
    private final JwtTokenProvider jwtTokenProvider;

    // CustomUserDetailsService: DB에서 회원 정보를 조회하는 서비스
    // JWT에서 추출한 ID로 실제 회원 정보를 가져올 때 사용
    private final CustomUserDetailsService customUserDetailsService;

    // MemberService: 회원 ID로 회원을 조회하기 위한 서비스
    // @Lazy: 순환 참조 방지 — 실제 사용 시점까지 빈 생성을 지연합니다.
    //   순환 경로: SecurityConfig → JwtAuthenticationFilter → MemberService → PasswordEncoder → SecurityConfig
    //   @Lazy를 붙이면 프록시 객체가 먼저 주입되고, 실제 호출 시에만 진짜 빈을 가져옵니다.
    private final MemberService memberService;

    // 생성자 — @RequiredArgsConstructor 대신 직접 작성 (MemberService에 @Lazy 적용하기 위해)
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   CustomUserDetailsService customUserDetailsService,
                                   @org.springframework.context.annotation.Lazy MemberService memberService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.customUserDetailsService = customUserDetailsService;
        this.memberService = memberService;
    }

    // ── doFilterInternal 메서드 ──────────────────────────────
    // OncePerRequestFilter의 핵심 메서드 — 요청마다 한 번 실행됩니다.
    //
    // 매개변수:
    //   - request: HTTP 요청 객체 (Authorization 헤더에서 토큰 추출)
    //   - response: HTTP 응답 객체
    //   - filterChain: 필터 체인 — 다음 필터로 요청을 전달할 때 사용
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // [1단계] /api/** 경로가 아니면 JWT 검증을 건너뜀
        // 타임리프 화면 요청(/, /members/login 등)은 세션 기반 인증을 사용하므로
        // JWT 검증이 불필요합니다.
        String requestURI = request.getRequestURI();
        if (!requestURI.startsWith("/api/")) {
            // 다음 필터로 바로 전달 (JWT 검증 생략)
            filterChain.doFilter(request, response);
            return;
        }

        // [2단계] Authorization 헤더에서 JWT 토큰 추출
        // HTTP 요청 헤더 예시: "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
        // "Bearer " 뒤의 문자열이 실제 JWT 토큰입니다.
        String token = extractTokenFromRequest(request);

        // [3단계] 토큰이 있고 유효한 경우 → 인증 정보 설정
        if (token != null && jwtTokenProvider.validateToken(token)) {
            try {
                // 토큰에서 회원 ID 추출
                Long memberId = jwtTokenProvider.getMemberIdFromToken(token);

                // ID로 회원을 조회하고 CustomUserDetails로 감싸기
                Member member = memberService.findById(memberId);
                UserDetails userDetails = new CustomUserDetails(member);

                // Spring Security의 인증 토큰 생성
                // UsernamePasswordAuthenticationToken: Spring Security가 사용하는 인증 객체
                //   ① userDetails: 인증된 사용자 정보 (principal)
                //   ② null: 비밀번호 (이미 JWT로 인증했으므로 불필요)
                //   ③ getAuthorities(): 사용자의 권한 목록 (ROLE_USER, ROLE_ADMIN)
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                // 요청 세부 정보 설정 (IP 주소, 세션 ID 등)
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                // SecurityContextHolder에 인증 정보 저장
                // → 이후 Controller에서 @LoginMember로 회원 정보 접근 가능
                // → SecurityConfig의 권한 검사에서 "인증됨"으로 판단
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // 회원 조회 실패 시 인증 설정 없이 통과 (다음 필터에서 처리)
                // 로그만 남기고 요청은 계속 진행
            }
        }

        // [4단계] 다음 필터로 요청 전달
        // 인증 설정 여부와 관계없이 다음 필터로 전달합니다.
        // 인증이 안 된 경우 SecurityConfig의 권한 설정에 의해 접근이 거부됩니다.
        filterChain.doFilter(request, response);
    }

    // ── extractTokenFromRequest 메서드 ───────────────────────
    // 역할: HTTP 요청의 Authorization 헤더에서 Bearer 토큰을 추출합니다.
    //
    // Authorization 헤더 형식: "Bearer eyJhbGciOiJIUzI1NiJ9..."
    // "Bearer " 접두사를 제거하고 순수한 토큰 문자열만 반환합니다.
    //
    private String extractTokenFromRequest(HttpServletRequest request) {
        // Authorization 헤더 값 가져오기
        String bearerToken = request.getHeader("Authorization");

        // "Bearer "로 시작하는 경우에만 토큰 추출
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            // "Bearer " (7글자) 이후의 문자열 = 실제 JWT 토큰
            return bearerToken.substring(7);
        }

        // Authorization 헤더가 없거나 Bearer 형식이 아닌 경우 null 반환
        return null;
    }
}