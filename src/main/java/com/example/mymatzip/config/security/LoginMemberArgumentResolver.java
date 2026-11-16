/*
 * ============================================================================
 *  파일명    : LoginMemberArgumentResolver.java
 *  위치      : com.example.mymatzip.config.security (보안 설정 패키지)
 *  역할      : @LoginMember 어노테이션이 붙은 매개변수에 현재 로그인 회원을 자동 주입하는 "해석기"
 * ============================================================================
 *
 *
 *  ■ 동작 흐름
 *    1) Controller 메서드가 호출될 때, Spring이 매개변수를 하나씩 검사
 *    2) @LoginMember + Member 타입인 매개변수를 발견하면 이 클래스가 동작
 *    3) SecurityContextHolder에서 현재 인증 정보(Authentication)를 가져옴
 *    4) 인증 정보에서 CustomUserDetails를 꺼내고, 그 안의 Member 객체를 반환
 *    5) Controller의 매개변수에 Member 객체가 자동으로 전달됨
 *
 *  ■ MVC에서의 위치
 *    Spring MVC의 "인자 해석기(ArgumentResolver)" 계층에 속합니다.
 *    [요청] → [Security 필터] → [ArgumentResolver(여기)] → [Controller 메서드]
 * ============================================================================
 */

// ── 패키지 선언 ──────────────────────────────────────────────
package com.example.mymatzip.config.security;

// ── import 문 ────────────────────────────────────────────────

// Member: 회원 엔티티 — 반환할 대상
import com.example.mymatzip.entity.Member;

// Authentication: Spring Security의 인증 정보 객체 (누가 로그인했는지 정보)
import org.springframework.security.core.Authentication;

// SecurityContextHolder: 현재 요청의 보안 정보(인증 정보)를 보관하는 저장소
import org.springframework.security.core.context.SecurityContextHolder;

// HandlerMethodArgumentResolver: Controller 메서드의 매개변수를 해석하는 인터페이스
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

// ── @Component ───────────────────────────────────────────────
// 이 클래스를 스프링 빈으로 등록합니다.
// WebMvcConfig에서 이 빈을 가져와 ArgumentResolver로 등록합니다.
@Component
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    // ── supportsParameter 메서드 ─────────────────────────────
    // 역할: "이 매개변수를 내가 처리할 수 있는가?"를 판단합니다.
    //
    // 조건 2가지가 모두 충족되면 true 반환:
    //   1) 매개변수에 @LoginMember 어노테이션이 붙어있는가?
    //   2) 매개변수의 타입이 Member인가?
    //
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // hasParameterAnnotation: 매개변수에 특정 어노테이션이 있는지 확인
        boolean hasAnnotation = parameter.hasParameterAnnotation(LoginMember.class);

        // getParameterType: 매개변수의 타입을 가져옴
        // isAssignableFrom: 해당 타입이 Member 클래스이거나 그 하위 클래스인지 확인
        boolean isMemberType = Member.class.isAssignableFrom(parameter.getParameterType());

        // 둘 다 true여야 이 resolver가 해당 매개변수를 처리함
        return hasAnnotation && isMemberType;
    }

    // ── resolveArgument 메서드 ────────────────────────────────
    // 역할: 실제로 매개변수에 넣어줄 값(Member 객체)을 가져옵니다.
    //
    // SecurityContextHolder에서 현재 인증 정보를 가져와서
    // CustomUserDetails 안에 있는 Member 객체를 꺼냅니다.
    //
    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        // [1단계] SecurityContextHolder에서 현재 인증 정보 가져오기
        // SecurityContextHolder: 현재 요청을 처리하는 스레드의 보안 정보를 보관
        // getContext(): 보안 컨텍스트(맥락)를 가져옴
        // getAuthentication(): 인증 객체(Authentication)를 가져옴
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // [2단계] 인증되지 않은 경우 null 반환
        // authentication이 null이거나, 인증되지 않았으면 → 로그인하지 않은 상태
        // → Controller에서 loginMember가 null로 전달됨
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        // [3단계] 인증 정보에서 principal(주체) 가져오기
        // getPrincipal(): 인증된 사용자 정보 객체를 반환
        // 우리가 만든 CustomUserDetails 또는 문자열("anonymousUser")일 수 있음
        Object principal = authentication.getPrincipal();

        // [4단계] CustomUserDetails인 경우에만 Member 추출
        // instanceof: principal이 CustomUserDetails 타입인지 확인
        if (principal instanceof CustomUserDetails) {
            // CustomUserDetails에서 원본 Member 엔티티를 꺼내서 반환
            return ((CustomUserDetails) principal).getMember();
        }

        // 그 외의 경우 (익명 사용자 등) → null 반환
        return null;
    }
}