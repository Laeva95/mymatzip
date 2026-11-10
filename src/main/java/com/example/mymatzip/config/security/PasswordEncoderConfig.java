// ============================================================
// [PasswordEncoderConfig.java] 비밀번호 암호화 설정 (별도 분리)
// ============================================================
// 역할: PasswordEncoder(비밀번호 암호화 도구) 빈을 등록합니다.
//
// 왜 SecurityConfig에서 분리했나?
//   SecurityConfig → JwtAuthenticationFilter → MemberService → PasswordEncoder → SecurityConfig
//   이런 "순환 참조(circular reference)" 문제가 발생하기 때문입니다.
//   PasswordEncoder를 별도 설정 클래스로 분리하면 순환이 끊어집니다.
// ============================================================

package com.example.mymatzip.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// @Configuration: 이 클래스가 스프링 설정 파일임을 선언
@Configuration
public class PasswordEncoderConfig {

    // ── PasswordEncoder 빈 등록 ──────────────────────────────
    // BCryptPasswordEncoder: 비밀번호를 BCrypt 알고리즘으로 암호화하는 인코더
    //
    // BCrypt란?
    //   비밀번호를 "해시(hash)"라는 되돌릴 수 없는 암호문으로 변환합니다.
    //   예: "1234" → "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl4in9GGvri"
    //   원본 비밀번호를 알아낼 수 없어 보안에 매우 강력합니다.
    //
    // 사용처:
    //   - 회원가입: 비밀번호를 BCrypt로 암호화하여 DB에 저장
    //   - 로그인: 입력한 비밀번호를 BCrypt로 변환 후, DB의 해시와 비교
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}