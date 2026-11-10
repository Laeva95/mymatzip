/*
 * ============================================================================
 *  파일명    : MemberViewController.java
 *  위치      : com.example.mymatzip.controller.view (뷰 컨트롤러 패키지)
 *  역할      : 회원 관련 화면(로그인, 회원가입 페이지)을 반환하는 "회원 안내 데스크"
 * ============================================================================
 *
 *
 *  ■ MVC에서의 위치
 *    Controller 계층 중 "View Controller" (화면 반환 컨트롤러)에 속합니다.
 *    이 컨트롤러는 비즈니스 로직 없이, 단순히 타임리프 화면만 반환합니다.
 *
 *    사용자 → [이 Controller] → [타임리프 화면 렌더링] → [HTML 반환]
 *                                      ↓
 *                           사용자가 폼에 입력 후 제출
 *                                      ↓
 *                           JavaScript가 MemberApiController 호출
 *
 *  ■ URL 매핑 요약
 *    기본 경로: /members
 *    ┌───────────────────┬────────┬──────────────────────────────┐
 *    │ URL               │ Method │ 설명                         │
 *    ├───────────────────┼────────┼──────────────────────────────┤
 *    │ /members/login    │ GET    │ 로그인 페이지 화면           │
 *    │ /members/signup   │ GET    │ 회원가입 페이지 화면         │
 *    └───────────────────┴────────┴──────────────────────────────┘
 * ============================================================================
 */

// ── 패키지 선언 ──────────────────────────────────────────────
package com.example.mymatzip.controller.view;

// ── import 문 (외부 라이브러리 가져오기) ─────────────────────

// @Controller: 이 클래스가 뷰(View) 컨트롤러임을 선언하는 어노테이션
// 메서드의 반환값(String)이 타임리프 화면 파일의 이름으로 해석됩니다.
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

// @GetMapping: HTTP GET 요청을 처리하는 메서드에 붙이는 어노테이션
// GET = 페이지를 "보여달라"는 요청 (브라우저 주소창에 URL 입력)
import org.springframework.web.bind.annotation.GetMapping;

// @RequestMapping: 이 컨트롤러의 기본 URL 경로를 설정하는 어노테이션
// 클래스에 붙이면 모든 메서드의 URL에 공통 접두사가 추가됨
import org.springframework.web.bind.annotation.RequestMapping;

// ── @Controller 어노테이션 ──────────────────────────────────
// 이 클래스가 뷰(View) 컨트롤러임을 선언합니다.
// @RestController와 달리 타임리프 화면 이름을 반환합니다.
@Controller

// ── @RequestMapping("/members") 어노테이션 ──────────────────
// 기본 URL 경로: /members
// 이 클래스의 모든 메서드 URL 앞에 "/members"가 자동으로 붙음
// 예: @GetMapping("/login") → 실제 URL은 "/members/login"
@RequestMapping("/members")
// ── 클래스 선언 ─────────────────────────────────────────────
// 이 클래스는 Service를 주입받지 않습니다.
// 왜냐하면 단순히 페이지만 보여주는 역할이기 때문입니다.
// 로그인/회원가입의 실제 처리는 MemberApiController + MemberService가 담당합니다.
public class MemberViewController {

    // [추가] 소셜 로그인 등록 정보 (application-google.properties 를 켰을 때만 존재)
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository;

    public MemberViewController(ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    // ── GET /members/login ──────────────────────────────────
    // 로그인 페이지: 이메일과 비밀번호를 입력하는 화면을 보여줌
    // 요청 예시: GET http://localhost:8080/members/login
    //   (브라우저 주소창에 직접 입력하거나, 로그인이 필요한 주소에서 Spring Security 가 리다이렉트할 때)
    // 응답: member/login.html 페이지를 HTML로 렌더링하여 반환
    //
    // @GetMapping("/login"): GET 방식으로 "/members/login" 요청이 오면 실행
    // 매개변수: 없음 (데이터를 전달할 필요 없이 빈 폼 화면만 보여주면 됨)
    // 반환값: String "member/login"
    //   → templates/member/login.html 파일을 렌더링
    @GetMapping("/login")
    public String loginPage(Model model) {
        // [추가] 켜져 있는 소셜 로그인 목록(예: ["google"])을 화면에 넘겨 버튼을 그립니다.
        //   등록 정보가 없으면 빈 목록 → 버튼이 보이지 않습니다.
        java.util.List<String> oauthProviders = new java.util.ArrayList<>();
        ClientRegistrationRepository registrations = clientRegistrationRepository.getIfAvailable();
        if (registrations instanceof Iterable<?> iterable) {
            for (Object registration : iterable) {
                oauthProviders.add(((ClientRegistration) registration).getRegistrationId());
            }
        }
        model.addAttribute("oauthProviders", oauthProviders);

        // "member/login"을 반환하면:
        //   뷰 리졸버(ViewResolver)가 prefix + "member/login" + suffix 조합
        //   → templates/member/login.html 파일을 찾아서 HTML로 렌더링
        //   사용자에게 이메일/비밀번호 입력 폼이 있는 로그인 화면을 보여줍니다.
        return "member/login";
    }

    // ── GET /members/signup ─────────────────────────────────
    // 회원가입 페이지: 이메일, 비밀번호, 닉네임 등을 입력하는 화면을 보여줌
    // 요청 예시: GET http://localhost:8080/members/signup
    //   (로그인 페이지에서 "회원가입" 링크를 클릭할 때)
    // 응답: member/signup.html 페이지를 HTML로 렌더링하여 반환
    //
    // @GetMapping("/signup"): GET 방식으로 "/members/signup" 요청이 오면 실행
    // 매개변수: 없음
    // 반환값: String "member/signup"
    //   → templates/member/signup.html 파일을 렌더링
    @GetMapping("/signup")
    public String signupPage() {
        // "member/signup"을 반환하면:
        //   → templates/member/signup.html 파일을 렌더링
        //   사용자에게 이메일, 비밀번호, 닉네임 입력 폼이 있는 회원가입 화면을 보여줍니다.
        return "member/signup";
    }
}