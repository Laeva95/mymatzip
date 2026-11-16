/*
 * ============================================================================
 *  파일명    : GlobalModelAdvice.java
 *  위치      : com.example.mymatzip.config.security (보안 설정 패키지)
 *  역할      : 모든 타임리프 화면에서 ${loginMember}로 로그인 회원 정보를 사용할 수 있게 해주는 "전역 모델 설정"
 * ============================================================================
 *
 *
 *  ■ 왜 필요한가?
 *    이전에는 session.setAttribute("loginMember", member)로 세션에 저장하고,
 *    타임리프 화면에서 ${sessionScope.loginMember}로 접근했습니다.
 *    Spring Security로 전환하면서, 인증 정보는 SecurityContextHolder에 저장됩니다.
 *    하지만 타임리프 화면에서는 SecurityContextHolder에 직접 접근하기 어려우므로,
 *    이 클래스가 자동으로 Model에 loginMember를 추가해줍니다.
 *
 *  ■ 동작 흐름
 *    1) 사용자가 어떤 페이지를 요청
 *    2) Controller가 실행되기 전에 이 @ModelAttribute가 자동 실행
 *    3) SecurityContextHolder에서 현재 로그인 회원 정보를 가져옴
 *    4) "loginMember"라는 이름으로 Model에 추가
 *    5) 타임리프 화면에서 ${loginMember.nickname} 등으로 접근 가능!
 * ============================================================================
 */

// ── 패키지 선언 ──────────────────────────────────────────────
package com.example.mymatzip.config.security;

// ── import 문 ────────────────────────────────────────────────

// Member: 회원 엔티티 — 모델에 추가할 대상
import com.example.mymatzip.entity.Member;

// Authentication: Spring Security의 인증 정보
import org.springframework.security.core.Authentication;

// SecurityContextHolder: 현재 보안 컨텍스트(인증 정보) 보관소
import org.springframework.security.core.context.SecurityContextHolder;

// @ControllerAdvice: 모든 Controller에 공통으로 적용되는 "전역 조언자"
// @RestControllerAdvice와 달리, 타임리프 화면를 반환하는 일반 Controller에도 적용됨
import org.springframework.web.bind.annotation.ControllerAdvice;

// @ModelAttribute: 이 메서드의 반환값을 자동으로 Model에 추가하는 어노테이션
import org.springframework.web.bind.annotation.ModelAttribute;

// ── @ControllerAdvice ────────────────────────────────────────
// 모든 Controller에 공통으로 적용됩니다.
// 이 클래스에 정의된 @ModelAttribute 메서드는 모든 요청에서 자동 실행됩니다.
@ControllerAdvice
public class GlobalModelAdvice {

    // ── loginMember() 메서드 ─────────────────────────────────
    // @ModelAttribute("loginMember"):
    //   이 메서드의 반환값을 "loginMember"라는 이름으로 Model에 자동 추가
    //   → 타임리프 화면에서 ${loginMember}로 접근 가능!
    //
    // 동작:
    //   1) SecurityContextHolder에서 Authentication(인증 정보) 가져오기
    //   2) 인증된 사용자인지 확인
    //   3) CustomUserDetails에서 Member 객체 추출
    //   4) Member 반환 → Model에 "loginMember"로 추가됨
    //
    // 반환값: Member 객체 (로그인 상태) 또는 null (비로그인 상태)
    @ModelAttribute("loginMember")
    public Member loginMember() {
        // SecurityContextHolder에서 인증 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증 정보가 있고, 인증된 상태이며, principal이 CustomUserDetails인 경우
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CustomUserDetails) {

            // CustomUserDetails에서 Member 엔티티 추출
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            return userDetails.getMember();
        }

        // 로그인하지 않은 상태 → null 반환 → 타임리프 화면에서 ${loginMember}는 null
        return null;
    }
}