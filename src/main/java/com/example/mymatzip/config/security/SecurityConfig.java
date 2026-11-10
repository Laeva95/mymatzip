/*
 * ============================================================================
 *  파일명    : SecurityConfig.java
 *  위치      : com.example.mymatzip.config.security (보안 설정 패키지)
 *  역할      : Spring Security의 핵심 설정 파일 — 애플리케이션의 "보안 총괄 관리자"
 * ============================================================================
 *
 *
 *  ■ 기존 LoginInterceptor와의 차이
 *    이전에는 LoginInterceptor(경비원 1명)가 로그인 여부만 확인했습니다.
 *    Spring Security는 "보안 회사 전체"를 고용한 것과 같습니다:
 *    - 비밀번호 암호화 (BCrypt)
 *    - CSRF 공격 방어
 *    - 역할별 접근 제어 (USER, ADMIN)
 *    - JWT 토큰 인증
 *    - 세션 관리
 *    이 모든 것을 이 설정 파일 하나로 관리합니다.
 *
 *  ■ MVC에서의 위치
 *    [브라우저] → [Spring Security 필터 체인(여기서 설정)] → [Controller] → [Service] → [DB]
 *    Controller에 도달하기 전에 보안 검사를 수행합니다.
 *
 *  ■ URL 매핑
 *    이 파일 자체는 URL을 처리하지 않습니다.
 *    어떤 URL에 어떤 보안 정책을 적용할지 "규칙"을 정의합니다.
 * ============================================================================
 */

// ── 패키지 선언 ──────────────────────────────────────────────
package com.example.mymatzip.config.security;

// ── import 문 (외부 라이브러리 가져오기) ─────────────────────

// @RequiredArgsConstructor: final 필드에 대한 생성자 자동 생성 (Lombok)
import lombok.RequiredArgsConstructor;

// @Bean: 이 메서드가 반환하는 객체를 스프링이 관리하는 "빈(Bean)"으로 등록
import org.springframework.context.annotation.Bean;

// @Configuration: 이 클래스가 스프링 설정 파일임을 표시
import org.springframework.context.annotation.Configuration;

// AuthenticationConfiguration: 인증(Authentication) 관련 설정을 가져오는 객체
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

// Spring Security 관련 설정 import
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

// DispatcherType: 요청의 종류(REQUEST, FORWARD, INCLUDE, ERROR 등)를 구분
// 타임리프 화면 포워딩 시 보안 필터를 우회하기 위해 사용
import jakarta.servlet.DispatcherType;
import org.springframework.http.HttpMethod;

// BCryptPasswordEncoder: 비밀번호를 안전하게 암호화하는 인코더
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// SecurityFilterChain: 보안 필터들의 체인(연쇄) — 요청이 통과해야 할 보안 검문소들
import org.springframework.security.web.SecurityFilterChain;

// CSRF 관련 import — CSRF(Cross-Site Request Forgery) 공격 방어용
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

// UsernamePasswordAuthenticationFilter: Spring Security의 기본 로그인 필터
// JWT 필터를 이 필터 "앞"에 배치하여 JWT 인증을 먼저 수행
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// PathPatternRequestMatcher: URL 패턴 매칭 — 로그아웃 요청 GET/POST 모두 허용하기 위해 사용
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

// [추가] 소셜 로그인(OAuth2) 설정용 — 11장의 클래스 대신 스프링 인터페이스 타입으로 받아서,
//   11장 파일이 없어도 이 파일이 컴파일되고 실행됩니다.
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

// ── @Configuration ───────────────────────────────────────────
// 이 클래스가 스프링의 설정 파일임을 선언합니다.
@Configuration

// ── @EnableWebSecurity ───────────────────────────────────────
// Spring Security를 활성화하는 어노테이션
// 이 어노테이션이 있어야 아래의 보안 설정이 실제로 적용됩니다.
@EnableWebSecurity

// ── @RequiredArgsConstructor ─────────────────────────────────
// final 필드에 대한 생성자를 자동 생성 → 의존성 주입(DI)
@RequiredArgsConstructor
public class SecurityConfig {

    // ── 필드 (의존성) ────────────────────────────────────────

    // CustomUserDetailsService: 사용자 인증 정보를 DB에서 가져오는 서비스
    // Spring Security가 로그인 시 이 서비스를 사용하여 회원 정보를 조회합니다.
    private final CustomUserDetailsService customUserDetailsService;

    // JwtAuthenticationFilter: JWT 토큰을 검증하는 필터
    // API 요청(/api/**)에서 Authorization 헤더의 JWT 토큰을 확인합니다.
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // [추가] 소셜 로그인 관련 빈 (있을 때만 사용)
    // ObjectProvider: 빈이 없어도 오류 없이 주입되고, getIfAvailable() 로 있으면 꺼내 씀
    //   - clientRegistrationRepository: application-google.properties 를 켰을 때만 스프링이 만듦
    //   - oAuth2UserService: 11장 CustomOAuth2UserService
    //   - oAuth2SuccessHandler: 11장 OAuth2LoginSuccessHandler
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository;
    private final ObjectProvider<OAuth2UserService<OAuth2UserRequest, OAuth2User>> oAuth2UserService;
    private final ObjectProvider<AuthenticationSuccessHandler> oAuth2SuccessHandler;

    // ── SecurityFilterChain 빈 등록 ──────────────────────────
    // 역할: HTTP 요청에 대한 보안 규칙을 설정하는 "필터 체인"을 만듭니다.
    //
    // @Bean: 이 메서드의 반환값(SecurityFilterChain)을 스프링이 관리하도록 등록
    // HttpSecurity http: 보안 설정을 위한 빌더 객체 (메서드 체이닝으로 설정)
    // throws Exception: 설정 중 예외 발생 가능
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // ── CSRF 설정 ────────────────────────────────────
                // CSRF(Cross-Site Request Forgery): 사이트 간 요청 위조 공격
                // CookieCsrfTokenRepository: CSRF 토큰을 쿠키에 저장 (JavaScript에서 읽기 가능)
                // ignoringRequestMatchers: API 경로는 JWT 토큰으로 인증하므로 CSRF 제외
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/api/**")
                )

                // ── 헤더 설정 ────────────────────────────────────
                // 같은 출처(sameOrigin)의 iframe 만 허용 (다른 사이트가 이 화면을 iframe 으로 넣는 공격 차단)
                .headers(headers -> headers.frameOptions(f -> f.sameOrigin()))

                // ── URL별 접근 권한 설정 ─────────────────────────
                .authorizeHttpRequests(auth -> auth
                        // ── FORWARD/INCLUDE/ERROR 디스패치 허용 ───────────
                        // Spring Security 6.x는 기본적으로 FORWARD 디스패치도 보안 검사를 합니다.
                        // 타임리프 화면를 렌더링할 때 Controller → 타임리프 화면으로 포워딩(forward)하는데,
                        // 이 포워딩까지 보안 검사를 하면 로그인 페이지 리다이렉트 루프가 발생합니다.
                        // 따라서 FORWARD, INCLUDE, ERROR 타입의 요청은 보안 검사를 생략합니다.
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ERROR).permitAll()

                        // permitAll(): 누구나 접근 가능 (로그인 안 해도 됨)
                        .requestMatchers(
                                "/",                        // 메인 페이지
                                "/members/login",           // 로그인 페이지
                                "/members/signup",          // 회원가입 페이지
                                "/api/members/login",       // 로그인 API
                                "/api/members/signup",      // 회원가입 API
                                "/css/**", "/js/**",        // CSS, JS 파일
                                "/images/**", "/uploads/**",// 이미지, 업로드 파일
                                "/error",                   // 에러 페이지
                                "/oauth2/**",               // OAuth2 소셜 로그인 경로
                                "/login/oauth2/**"          // OAuth2 콜백 경로
                        ).permitAll()

                        // hasRole("ADMIN"): ADMIN 역할을 가진 회원만 접근 가능
                        // [팀 프로젝트] S3 사진 보기(GET /api/files/파일이름)는 누구나. 업로드(POST)는 아래 anyRequest 규칙으로 로그인 필요
                        .requestMatchers(HttpMethod.GET, "/api/files/**").permitAll()
                        .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")

                        // anyRequest().authenticated(): 위에서 지정하지 않은 모든 URL은 로그인 필요
                        .anyRequest().authenticated()
                )

                // ── 폼 로그인 설정 ───────────────────────────────
                // formLogin: 로그인 페이지 설정
                // loginPage("/members/login"): 커스텀 로그인 페이지 경로 지정
                //   (Spring Security 기본 로그인 페이지 대신 우리가 만든 페이지 사용)
                // loginProcessingUrl("/perform-login"): Spring Security의 기본 폼 로그인 처리 URL 설정
                //   실제 로그인은 /api/members/login (REST API)에서 직접 처리하므로,
                //   사용되지 않는 별도 경로를 지정하여 URL 충돌 & 리다이렉트 루프를 방지
                // defaultSuccessUrl: 폼 로그인 성공 후 기본 이동 페이지
                .formLogin(form -> form
                        .loginPage("/members/login")
                        .loginProcessingUrl("/perform-login")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )

                // ── 로그아웃 설정 ────────────────────────────────
                // logoutRequestMatcher: GET/POST 모두 로그아웃 허용
                //   (기본은 POST만 허용하지만, <a> 링크(GET)로도 로그아웃 가능하도록 설정)
                // logoutSuccessUrl: 로그아웃 성공 후 이동할 페이지
                // invalidateHttpSession: 세션 무효화 (모든 세션 데이터 삭제)
                // clearAuthentication: SecurityContext에서 인증 정보도 삭제
                .logout(logout -> logout
                        .logoutRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher("/api/members/logout"))
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                )

                // ── 세션 관리 설정 ───────────────────────────────
                // IF_REQUIRED: 필요할 때만 세션 생성 (타임리프 화면를 위한 세션 + API는 JWT 사용)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )

                // ── JWT 필터 등록 ────────────────────────────────
                // addFilterBefore: JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 배치
                // API 요청이 오면 JWT 토큰을 먼저 검증한 후, 나머지 보안 처리를 진행
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // ── [추가] 소셜 로그인 (구글) ────────
        // 클라이언트 등록 정보가 있을 때만 켭니다. 없으면(기본 실행) 이 부분을 건너뛰므로
        // 키 없이도 애플리케이션이 그대로 실행됩니다. (11장 참고)
        if (clientRegistrationRepository.getIfAvailable() != null) {
            http.oauth2Login(oauth -> {
                oauth.loginPage("/members/login");                 // 로그인 화면은 우리 화면
                OAuth2UserService<OAuth2UserRequest, OAuth2User> userService = oAuth2UserService.getIfAvailable();
                if (userService != null) {
                    // 소셜 서비스가 준 사용자 정보로 우리 DB 회원을 찾거나 만듦
                    oauth.userInfoEndpoint(userInfo -> userInfo.userService(userService));
                }
                AuthenticationSuccessHandler successHandler = oAuth2SuccessHandler.getIfAvailable();
                if (successHandler != null) {
                    oauth.successHandler(successHandler);           // 성공 후 홈으로
                } else {
                    oauth.defaultSuccessUrl("/", true);
                }
            });
        }

        return http.build();
    }

    // ── AuthenticationManager 빈 등록 ────────────────────────
    // AuthenticationManager: 인증(로그인)을 총괄하는 관리자 객체
    // Spring Security가 로그인 요청을 처리할 때 이 객체를 사용합니다.
    // AuthenticationConfiguration에서 기본 설정된 매니저를 가져옵니다.
    //
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}