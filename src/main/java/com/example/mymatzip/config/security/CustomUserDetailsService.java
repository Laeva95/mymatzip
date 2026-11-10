/*
 * ============================================================================
 *  파일명    : CustomUserDetailsService.java
 *  위치      : com.example.mymatzip.config.security (보안 설정 패키지)
 *  역할      : Spring Security의 로그인 처리 시 DB에서 회원 정보를 조회하는 "회원 조회 서비스"
 * ============================================================================
 *
 *
 *  ■ 동작 흐름
 *    1) 사용자가 이메일/비밀번호로 로그인 요청
 *    2) Spring Security가 이 서비스의 loadUserByUsername(email)을 호출
 *    3) DB에서 이메일로 회원을 조회
 *    4) 찾으면 CustomUserDetails로 감싸서 반환
 *    5) Spring Security가 비밀번호 비교 등 나머지 인증 처리를 진행
 *
 *  ■ MVC에서의 위치
 *    Spring Security 내부에서 사용되는 서비스입니다.
 *    [로그인 요청] → [Spring Security] → [이 서비스(DB 조회)] → [인증 처리]
 * ============================================================================
 */

// ── 패키지 선언 ──────────────────────────────────────────────
package com.example.mymatzip.config.security;

// ── import 문 ────────────────────────────────────────────────

// Member: 회원 엔티티 — DB에서 조회한 회원 정보를 담는 객체
import com.example.mymatzip.entity.Member;

// MemberRepository: DB의 member 테이블에 접근하는 저장소 인터페이스
import com.example.mymatzip.repository.MemberRepository;

// @RequiredArgsConstructor: final 필드에 대한 생성자 자동 생성
import lombok.RequiredArgsConstructor;

// @Service: 이 클래스가 서비스 계층임을 Spring에게 알려주는 어노테이션
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// ── @Service ─────────────────────────────────────────────────
// Spring Security가 UserDetailsService 타입의 빈을 자동으로 찾아서 사용합니다.
@Service

// ── @RequiredArgsConstructor ─────────────────────────────────
// final 필드(memberRepository)에 대한 생성자를 자동 생성 → 의존성 주입(DI)
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    // ── 필드 (의존성) ────────────────────────────────────────
    // MemberRepository: DB에서 회원 정보를 조회하기 위한 저장소
    private final MemberRepository memberRepository;

    // ── loadUserByUsername 메서드 ─────────────────────────────
    // 역할: 로그인 시 이메일(username)로 DB에서 회원 정보를 조회합니다.
    //
    // Spring Security가 자동으로 호출하는 메서드입니다.
    // UserDetailsService 인터페이스의 유일한 메서드이며, 반드시 구현해야 합니다.
    //
    // 매개변수: email — 사용자가 로그인 시 입력한 이메일 (Spring Security에서는 "username")
    // 반환값: UserDetails — Spring Security가 이해할 수 있는 사용자 정보 객체
    // 예외: UsernameNotFoundException — 해당 이메일의 회원이 없을 때 발생
    //
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // [1단계] DB에서 이메일로 회원 조회
        // memberRepository.findByEmail(email): 이메일로 회원 검색
        // .orElseThrow(): 없으면 예외 발생
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("등록되지 않은 이메일입니다: " + email));

        // [2단계] Member를 CustomUserDetails로 감싸서 반환
        // Spring Security가 이해할 수 있는 형태(UserDetails)로 변환
        return new CustomUserDetails(member);
    }
}