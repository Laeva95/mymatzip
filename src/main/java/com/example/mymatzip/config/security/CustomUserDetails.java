/*
 * ============================================================================
 *  파일명    : CustomUserDetails.java
 *  위치      : com.example.mymatzip.config.security (보안 설정 패키지)
 *  역할      : Spring Security가 이해할 수 있는 "회원 정보 래퍼(wrapper)"
 * ============================================================================
 *
 *
 *    우리 앱: Member 객체 (email, password, nickname, role)
 *    Spring Security: UserDetails 인터페이스 (username, password, authorities)
 *
 *    둘의 형식이 다르기 때문에, 이 클래스가 중간에서 "번역"해줍니다.
 *
 *  ■ MVC에서의 위치
 *    Spring Security 내부에서 인증(로그인) 처리 시 사용됩니다.
 *    [로그인 요청] → [CustomUserDetailsService] → [CustomUserDetails 생성] → [Security 인증]
 *
 *  ■ 주요 메서드
 *    getAuthorities(): 회원의 권한(역할) 반환 — "ROLE_USER" 또는 "ROLE_ADMIN"
 *    getUsername(): 회원의 로그인 ID(이메일) 반환
 *    getPassword(): 회원의 암호화된 비밀번호 반환
 *    getMember(): 원본 Member 엔티티 반환 (우리 앱에서 사용)
 * ============================================================================
 */

// ── 패키지 선언 ──────────────────────────────────────────────
package com.example.mymatzip.config.security;

// ── import 문 ────────────────────────────────────────────────

// Member: 우리 앱의 회원 엔티티 — 이 클래스가 감싸는(wrap) 대상
import com.example.mymatzip.entity.Member;

// @Getter: Lombok이 getter 메서드를 자동 생성 (getMember() 등)
import lombok.Getter;

// @RequiredArgsConstructor: final 필드에 대한 생성자 자동 생성
import lombok.RequiredArgsConstructor;

// GrantedAuthority: Spring Security에서 "권한"을 나타내는 인터페이스
import org.springframework.security.core.GrantedAuthority;

// SimpleGrantedAuthority: GrantedAuthority의 간단한 구현체
// 문자열("ROLE_USER")을 권한 객체로 변환
import org.springframework.security.core.authority.SimpleGrantedAuthority;

// UserDetails: Spring Security가 인증에 사용하는 사용자 정보 인터페이스
// 이 인터페이스를 구현해야 Spring Security가 우리 회원을 인식합니다.
import org.springframework.security.core.userdetails.UserDetails;

// Collection: 여러 개의 데이터를 담는 자료구조의 최상위 인터페이스
import java.util.Collection;

// List: 순서가 있는 목록 자료구조
import java.util.List;

// Map: 키-값 쌍을 저장하는 자료구조
import java.util.Map;

// OAuth2User: 소셜 로그인 사용자 정보를 담는 Spring Security 인터페이스
import org.springframework.security.oauth2.core.user.OAuth2User;

// ── @Getter ──────────────────────────────────────────────────
// getMember() 메서드를 자동 생성 — 원본 Member 엔티티에 접근할 때 사용
@Getter

// ── @RequiredArgsConstructor ─────────────────────────────────
// final 필드(member)에 대한 생성자를 자동 생성:
//   public CustomUserDetails(Member member) { this.member = member; }
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails, OAuth2User {

    // ── 필드 ─────────────────────────────────────────────────
    // member: 우리 앱의 회원 엔티티 객체
    // 이 객체를 감싸서(wrap) Spring Security가 이해할 수 있는 형태로 제공
    private final Member member;

    // attributes: 소셜 로그인(OAuth2) 시 제공되는 사용자 속성 정보
    // 일반 로그인 시에는 빈 맵, 소셜 로그인 시에는 소셜 서비스가 제공한 정보가 담김
    private Map<String, Object> attributes = Map.of();

    // ── getAuthorities() ─────────────────────────────────────
    // 역할: 이 사용자가 가진 "권한(역할) 목록"을 반환합니다.
    // Spring Security는 이 메서드로 ADMIN인지 USER인지 확인합니다.
    //
    // member.getRole().name(): Role enum의 이름을 문자열로 가져옴
    //   Role.USER → "USER", Role.ADMIN → "ADMIN"
    // "ROLE_" 접두사: Spring Security의 규칙!
    //   hasRole("ADMIN") → 내부적으로 "ROLE_ADMIN"을 찾음
    //   따라서 권한 문자열은 반드시 "ROLE_"로 시작해야 합니다.
    //
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()));
    }

    // ── getPassword() ────────────────────────────────────────
    // 역할: 암호화된 비밀번호를 반환합니다.
    // Spring Security가 로그인 시 입력한 비밀번호와 이 값을 비교합니다.
    @Override
    public String getPassword() {
        return member.getPassword();
    }

    // ── getUsername() ─────────────────────────────────────────
    // 역할: 로그인 ID(이메일)를 반환합니다.
    // Spring Security에서 "username"은 로그인에 사용하는 식별자입니다.
    // 우리 앱에서는 이메일을 로그인 ID로 사용하므로 이메일을 반환합니다.
    @Override
    public String getUsername() {
        return member.getEmail();
    }

    // ── 계정 상태 관련 메서드들 ───────────────────────────────
    // Spring Security가 계정의 상태를 확인하는 메서드들입니다.
    // 모두 true를 반환하면 "정상 계정"으로 인식합니다.
    // 향후 계정 잠금, 만료 등의 기능이 필요하면 이 메서드들을 수정합니다.

    // 계정이 만료되지 않았는가? → true (만료되지 않음)
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    // 계정이 잠기지 않았는가? → true (잠기지 않음)
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    // 인증 정보(비밀번호)가 만료되지 않았는가? → true (만료되지 않음)
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // 계정이 활성화되어 있는가? → true (활성화됨)
    @Override
    public boolean isEnabled() {
        return true;
    }

    // ── OAuth2User 인터페이스 구현 메서드들 ─────────────────────
    // 소셜 로그인(Google)을 통해 로그인한 사용자의 정보를 제공합니다.

    // getAttributes(): 소셜 로그인 서비스가 제공한 사용자 속성(프로필 정보)을 반환
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    // getName(): 사용자의 고유 식별자(이메일)를 반환
    @Override
    public String getName() {
        return member.getEmail();
    }

    // attributes 설정 메서드 — 소셜 로그인 시 사용
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
}