/*
 * ============================================================================
 *  파일명    : LoginMember.java
 *  위치      : com.example.mymatzip.config.security (보안 설정 패키지)
 *  역할      : 컨트롤러 메서드에서 현재 로그인한 회원 정보를 쉽게 받을 수 있게 해주는 "커스텀 어노테이션"
 * ============================================================================
 *
 *
 *  ■ 사용 전 (기존 방식 — 세션에서 직접 꺼내기)
 *    public ResponseEntity<?> create(..., HttpSession session) {
 *        Member loginMember = (Member) session.getAttribute("loginMember");
 *    }
 *
 *  ■ 사용 후 (이 어노테이션 사용 — 깔끔하고 안전)
 *    public ResponseEntity<?> create(..., @LoginMember Member loginMember) {
 *        // loginMember가 자동으로 주입됨!
 *    }
 *
 *  ■ 동작 원리
 *    이 어노테이션은 LoginMemberArgumentResolver와 함께 동작합니다.
 *    1) Controller 메서드 호출 시 @LoginMember가 붙은 매개변수를 발견
 *    2) LoginMemberArgumentResolver가 SecurityContextHolder에서 인증 정보를 가져옴
 *    3) 인증 정보에서 Member 객체를 추출하여 매개변수에 주입
 * ============================================================================
 */

// ── 패키지 선언 ──────────────────────────────────────────────
package com.example.mymatzip.config.security;

// ── import 문 ────────────────────────────────────────────────

// ElementType.PARAMETER: 이 어노테이션을 "메서드의 매개변수"에만 붙일 수 있음
import java.lang.annotation.ElementType;

// RetentionPolicy.RUNTIME: 프로그램 실행 중(런타임)에도 이 어노테이션 정보를 유지
// RUNTIME이어야 Spring이 실행 중에 이 어노테이션을 읽을 수 있습니다.
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// @Target: 이 어노테이션을 어디에 붙일 수 있는지 제한하는 메타 어노테이션
import java.lang.annotation.Target;

// ── @Target(ElementType.PARAMETER) ──────────────────────────
// 이 어노테이션은 "매개변수(파라미터)"에만 붙일 수 있습니다.
// 클래스, 필드, 메서드 등에는 붙일 수 없습니다.
@Target(ElementType.PARAMETER)

// ── @Retention(RetentionPolicy.RUNTIME) ─────────────────────
// 이 어노테이션 정보를 프로그램 실행 중(런타임)에도 유지합니다.
// Spring이 실행 중에 이 어노테이션이 붙은 매개변수를 찾아서 처리합니다.
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginMember {
    // 어노테이션 내부에 별도의 속성(값)은 없습니다.
    // 단순히 "이 매개변수에 로그인 회원 정보를 넣어달라"는 표시 역할만 합니다.
}