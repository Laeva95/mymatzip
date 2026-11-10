/*
 * ============================================================================
 *  파일명    : MemberApiController.java
 *  위치      : com.example.mymatzip.controller.api (API 컨트롤러 패키지)
 *  역할      : 회원 관련 API 요청을 처리하는 "회원 접수 창구" (REST API)
 * ============================================================================
 *
 *
 *  ■ MVC에서의 위치
 *    Controller 계층에 속합니다.
 *    [브라우저(JavaScript)] → [이 Controller] → [MemberService] → [MemberRepository] → [DB]
 *    브라우저에서 JavaScript(fetch/ajax)로 JSON 데이터를 보내면,
 *    이 Controller가 받아서 Service에 전달하고, 결과를 JSON으로 응답합니다.
 *
 *  ■ REST API란?
 *    화면(HTML)을 반환하지 않고, 데이터(JSON)만 반환하는 방식입니다.
 *    브라우저의 JavaScript가 이 데이터를 받아서 화면에 표시합니다.
 *
 *  ■ [Security] 변경 사항
 *    - 로그인: JWT 토큰을 발급하고, SecurityContext에 인증 정보를 설정
 *    - 로그아웃: Spring Security가 SecurityConfig에서 설정한 대로 자동 처리
 *    - 세션: session.setAttribute 대신 SecurityContextHolder 사용
 *
 *  ■ URL 매핑 요약
 *    기본 경로: /api/members
 *    ┌──────────────────────┬────────┬──────────────────────────────┐
 *    │ URL                  │ Method │ 설명                         │
 *    ├──────────────────────┼────────┼──────────────────────────────┤
 *    │ /api/members/signup  │ POST   │ 회원가입                     │
 *    │ /api/members/login   │ POST   │ 로그인 (JWT 토큰 발급)       │
 *    │ /api/members/logout  │ GET    │ 로그아웃 (Security 자동 처리) │
 *    └──────────────────────┴────────┴──────────────────────────────┘
 * ============================================================================
 */

// ── 패키지 선언 ──────────────────────────────────────────────
// 이 파일이 controller.api 패키지에 속함을 선언합니다.
// api 패키지: JSON 데이터를 주고받는 REST API 컨트롤러들이 모여있는 곳
package com.example.mymatzip.controller.api;

// ── import 문 (외부 라이브러리 가져오기) ─────────────────────

// LoginForm: 로그인 시 사용하는 데이터 전달 객체 (DTO)
// 이메일과 비밀번호 필드를 가지고 있음
import com.example.mymatzip.dto.LoginForm;

// MemberSignupForm: 회원가입 시 사용하는 데이터 전달 객체 (DTO)
// 이메일, 비밀번호, 닉네임 등의 필드를 가지고 있음
import com.example.mymatzip.dto.MemberSignupForm;

// Member: 회원 정보를 담는 엔티티(Entity) 클래스
// DB의 member 테이블과 1:1로 매핑되는 자바 객체
import com.example.mymatzip.entity.Member;

// MemberService: 회원 관련 비즈니스 로직을 처리하는 서비스 클래스
// 실제 회원가입, 로그인 로직은 이 서비스가 담당합니다.
import com.example.mymatzip.service.MemberService;

// [Security] JwtTokenProvider: JWT 토큰 생성/검증 유틸리티
import com.example.mymatzip.config.security.JwtTokenProvider;

// [Security] CustomUserDetails: Spring Security가 이해하는 사용자 정보 래퍼
import com.example.mymatzip.config.security.CustomUserDetails;

// [Security] Spring Security 관련 import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

// HttpServletRequest, HttpServletResponse: 세션에 SecurityContext를 저장하기 위해 필요
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// @Valid: 입력값 유효성 검증을 활성화하는 어노테이션
// DTO 클래스의 @NotBlank, @Email 등의 검증 조건을 실행합니다.
import jakarta.validation.Valid;

// @RequiredArgsConstructor: final 필드에 대한 생성자를 자동 생성해주는 Lombok 어노테이션
// 직접 생성자를 작성하지 않아도 됩니다. (코드 줄이기 용도)
import lombok.RequiredArgsConstructor;

// HttpStatus: HTTP 상태 코드를 정의하는 열거형(enum)
// 200(OK), 201(CREATED), 400(BAD_REQUEST), 401(UNAUTHORIZED) 등
import org.springframework.http.HttpStatus;

// ResponseEntity: HTTP 응답 전체(상태코드 + 헤더 + 본문)를 담는 객체
import org.springframework.http.ResponseEntity;

// @PostMapping: HTTP POST 요청을 처리하는 메서드에 붙이는 어노테이션
// POST = 데이터를 "전송/생성"할 때 사용 (회원가입, 로그인 등)
import org.springframework.web.bind.annotation.PostMapping;

// @RequestBody: HTTP 요청의 본문(body)에 있는 JSON 데이터를 자바 객체로 변환
import org.springframework.web.bind.annotation.RequestBody;

// @RequestMapping: 이 컨트롤러의 기본 URL 경로를 설정하는 어노테이션
// 클래스에 붙이면 이 클래스의 모든 메서드에 공통 접두사(prefix)가 붙음
import org.springframework.web.bind.annotation.RequestMapping;

// @RestController: 이 클래스가 REST API 컨트롤러임을 선언
// @Controller + @ResponseBody를 합친 것
// 모든 메서드의 반환값이 자동으로 JSON으로 변환되어 응답됩니다.
import org.springframework.web.bind.annotation.RestController;

// Map: 키-값 쌍으로 데이터를 저장하는 자료구조
// JSON 응답을 만들 때 사용 (예: Map.of("error", "에러 메시지") → {"error": "에러 메시지"})
import java.util.Map;

// ── @RestController 어노테이션 ──────────────────────────────
// 이 클래스가 REST API 컨트롤러임을 스프링에게 알려줍니다.
// REST = 화면(HTML)이 아닌 데이터(JSON)를 반환하는 방식
// 이 어노테이션이 있으면 메서드의 반환값이 자동으로 JSON으로 변환됩니다.
@RestController

// ── @RequestMapping("/api/members") 어노테이션 ─────────────
// 이 컨트롤러의 기본 URL 경로를 "/api/members"로 설정합니다.
// 이 클래스 안의 모든 메서드 URL 앞에 자동으로 "/api/members"가 붙습니다.
// 예: @PostMapping("/signup") → 실제 URL은 "/api/members/signup"
@RequestMapping("/api/members")

// ── @RequiredArgsConstructor 어노테이션 ─────────────────────
// Lombok 라이브러리가 제공하는 어노테이션입니다.
// final로 선언된 필드에 대해 생성자를 자동으로 만들어줍니다.
// 이를 통해 스프링이 자동으로 MemberService, JwtTokenProvider를 주입(DI)합니다.
@RequiredArgsConstructor
// ── 클래스 선언 ─────────────────────────────────────────────
// public: 다른 패키지에서도 접근 가능
// class MemberApiController: "회원 API 컨트롤러"라는 이름의 클래스
public class MemberApiController {

    // ── 필드 (멤버 변수) ────────────────────────────────────
    // private: 이 클래스 내부에서만 접근 가능
    // final: 한 번 설정되면 변경 불가 (안전성 보장)
    // MemberService: 회원 관련 비즈니스 로직을 처리하는 서비스 객체
    // @RequiredArgsConstructor에 의해 생성자 주입(DI)으로 자동 주입됩니다.
    private final MemberService memberService;

    // [Security] JwtTokenProvider: JWT 토큰을 생성하는 도구
    // 로그인 성공 시 JWT 토큰을 발급하여 클라이언트에게 전달합니다.
    private final JwtTokenProvider jwtTokenProvider;

    // ── POST /api/members/signup ────────────────────────────
    // 회원가입 API: JSON으로 받은 회원 정보를 DB에 저장
    // 요청 예시: POST http://localhost:8080/api/members/signup
    // 요청 Body: {"email":"test@test.com","password":"1234","nickname":"테스트"}
    // 성공 응답: 201 Created + {"id":1,"email":"test@test.com","nickname":"테스트"}
    // 실패 응답: 400 Bad Request + {"error":"이미 사용 중인 이메일입니다."}
    //
    // [Security] 비밀번호는 MemberService에서 BCrypt로 암호화되어 DB에 저장됩니다.
    // 예외 처리는 GlobalExceptionHandler가 담당하므로, try-catch 없이 깔끔하게 처리합니다.
    //
    // @PostMapping("/signup"): POST 방식으로 "/api/members/signup" 요청이 오면 이 메서드 실행
    // 매개변수:
    //   - @Valid: MemberSignupForm의 유효성 검증 활성화
    //     (@NotBlank, @Email, @Size 등의 조건을 검사)
    //   - @RequestBody: 요청 본문(JSON)을 MemberSignupForm 객체로 변환
    //   - form: 변환된 회원가입 데이터가 담긴 객체
    // 반환값: ResponseEntity<?> (상태코드 + JSON 응답)
    //   - ?는 와일드카드: 성공 시 Map, 실패 시 Map 등 다양한 타입 가능
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody MemberSignupForm form) {
        // ── 1단계: 회원가입 처리 ────────────────────────
        // memberService.signup(form): Service에게 회원가입 로직 실행을 요청
        //   - 이메일 중복 검사
        //   - 비밀번호 BCrypt 암호화
        //   - DB에 회원 정보 저장
        // 반환값: 저장된 회원(Member) 객체 (ID가 자동 생성되어 있음)
        // ※ 예외 발생 시 GlobalExceptionHandler가 자동으로 에러 응답을 반환합니다.
        Member member = memberService.signup(form);

        // ── 2단계: 성공 응답 반환 ───────────────────────
        // ResponseEntity.status(HttpStatus.CREATED): HTTP 상태코드 201(Created) 설정
        //   201 = "새로운 리소스가 성공적으로 생성되었습니다"
        // .body(Map.of(...)): 응답 본문에 회원 정보를 JSON으로 담아 반환
        //   Map.of("id", ..., "email", ..., "nickname", ...):
        //   → {"id": 1, "email": "test@test.com", "nickname": "테스트"}
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", member.getId(),
                "email", member.getEmail(),
                "nickname", member.getNickname()
        ));
    }

    // ── POST /api/members/login ─────────────────────────────
    // 로그인 API: 이메일과 비밀번호를 확인하여 JWT 토큰 발급 + SecurityContext 설정
    // 요청 예시: POST http://localhost:8080/api/members/login
    // 요청 Body: {"email":"test@test.com","password":"1234"}
    // 성공 응답: 200 OK + {"id":1,"email":"...","nickname":"...","token":"eyJ..."}
    // 실패 응답: 401 Unauthorized + {"error":"비밀번호가 일치하지 않습니다."}
    //
    // [Security] 변경 사항:
    //   - 이전: HttpSession에 loginMember를 저장 (세션 기반)
    //   - 현재: JWT 토큰 발급 + SecurityContextHolder에 인증 정보 설정
    //     → REST API 클라이언트: JWT 토큰으로 인증
    //     → 타임리프 화면: SecurityContext + 세션으로 인증
    //
    // @PostMapping("/login"): POST 방식으로 "/api/members/login" 요청이 오면 실행
    // 매개변수:
    //   - @Valid @RequestBody LoginForm form: 로그인 폼 데이터 (이메일, 비밀번호)
    // 반환값: ResponseEntity<?> (상태코드 + JSON 응답)
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginForm form,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        try {
            // ── 1단계: 로그인 처리 ──────────────────────────
            // memberService.login(form): Service에게 로그인 검증 요청
            //   - 이메일로 회원 조회
            //   - BCrypt로 비밀번호 일치 여부 확인
            //   - 일치하면 Member 객체 반환, 불일치하면 예외 발생
            Member member = memberService.login(form);

            // ── 2단계: [Security] SecurityContext에 인증 정보 설정 ──
            // Spring Security의 SecurityContextHolder에 인증 정보를 저장합니다.
            // → 타임리프 화면에서 ${loginMember}로 로그인 회원 정보에 접근 가능
            // → GlobalModelAdvice가 SecurityContext에서 Member를 꺼내 Model에 추가
            //
            // CustomUserDetails: Member를 Spring Security가 이해하는 형태로 감싸기
            CustomUserDetails userDetails = new CustomUserDetails(member);

            // UsernamePasswordAuthenticationToken: Spring Security의 인증 객체
            //   ① userDetails: 인증된 사용자 정보
            //   ② null: 비밀번호 (이미 검증 완료)
            //   ③ getAuthorities(): 권한 목록 (ROLE_USER 또는 ROLE_ADMIN)
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

            // SecurityContext를 새로 생성하고 인증 정보를 설정
            // Spring Security 6.x에서는 SecurityContext를 명시적으로 세션에 저장해야 합니다.
            // (5.x에서는 자동 저장되었지만, 6.x에서는 보안 강화를 위해 명시적 저장 필요)
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            // HttpSession에 SecurityContext 저장 → 이후 요청에서도 인증 상태 유지
            new HttpSessionSecurityContextRepository().saveContext(context, request, response);

            // ── 3단계: [Security] JWT 토큰 발급 ──────────────
            // jwtTokenProvider.generateToken(member): 회원 정보를 담은 JWT 토큰 생성
            // 이 토큰은 클라이언트가 보관하고, 이후 API 요청 시 Authorization 헤더에 포함
            String token = jwtTokenProvider.generateToken(member);

            // ── 4단계: 성공 응답 반환 (회원 정보 + JWT 토큰) ──
            // ResponseEntity.ok(...): HTTP 상태코드 200(OK) + 응답 본문
            // Map.of(...): {"id": 1, "email": "...", "nickname": "...", "token": "eyJ..."} JSON 생성
            // 클라이언트는 이 token을 localStorage 등에 저장하고,
            // 이후 API 호출 시 "Authorization: Bearer 토큰값" 헤더에 포함합니다.
            return ResponseEntity.ok(Map.of(
                    "id", member.getId(),
                    "email", member.getEmail(),
                    "nickname", member.getNickname(),
                    "token", token
            ));
        } catch (IllegalArgumentException e) {
            // ── 5단계: 실패 시 에러 응답 반환 ───────────────
            // HTTP 상태코드 401(Unauthorized): "인증 실패" (이메일 또는 비밀번호 틀림)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    // ── 로그아웃 안내 ─────────────────────────────────────────
    // 로그아웃은 Spring Security가 자동으로 처리합니다.
    // SecurityConfig에서 다음과 같이 설정되어 있습니다:
    //   .logout(logout -> logout
    //       .logoutUrl("/api/members/logout")      ← 이 URL로 요청하면 로그아웃
    //       .logoutSuccessUrl("/")                  ← 로그아웃 후 메인 페이지로 이동
    //       .invalidateHttpSession(true)            ← 세션 무효화
    //   )
    // 따라서 별도의 로그아웃 메서드를 작성하지 않아도 됩니다!
}