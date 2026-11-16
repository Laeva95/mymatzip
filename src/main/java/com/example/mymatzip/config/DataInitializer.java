/*
 * ============================================================================
 *  파일명    : DataInitializer.java
 *  위치      : com.example.mymatzip.config (설정 패키지)
 *  역할      : 서버 시작 시 data.sql로 생성된 회원들의 비밀번호를 BCrypt로 암호화하는 "초기화 도우미"
 * ============================================================================
 *
 *  ■ 왜 필요한가?
 *    data.sql에서 회원을 INSERT할 때 비밀번호를 평문('1234')으로 넣습니다.
 *    하지만 Spring Security는 BCrypt로 암호화된 비밀번호를 기대합니다.
 *    따라서 서버 시작 직후, 평문 비밀번호를 BCrypt 해시로 변환해야 합니다.
 *
 *
 *  ■ 실행 시점
 *    CommandLineRunner를 구현하므로, Spring Boot가 완전히 시작된 후 run() 메서드가 자동 실행됩니다.
 *    순서: 테이블 생성 → data.sql INSERT → 이 클래스의 run() 실행
 *
 *  ■ BCrypt 감지 로직
 *    이미 BCrypt로 암호화된 비밀번호는 "$2a$"로 시작합니다.
 *    "$2a$"로 시작하지 않는 비밀번호만 암호화합니다.
 *    → 서버를 여러 번 재시작해도 이중 암호화되지 않습니다.
 * ============================================================================
 */

// ── 패키지 선언 ──────────────────────────────────────────────
package com.example.mymatzip.config;

// ── import 문 ────────────────────────────────────────────────

// MemberRepository: 회원 정보를 DB에서 조회/수정하는 저장소
import com.example.mymatzip.repository.MemberRepository;

// @RequiredArgsConstructor: final 필드에 대한 생성자 자동 생성
import lombok.RequiredArgsConstructor;

// CommandLineRunner: Spring Boot 시작 완료 후 자동 실행되는 인터페이스
import org.springframework.boot.CommandLineRunner;

// PasswordEncoder: 비밀번호 암호화 인터페이스 (BCryptPasswordEncoder가 구현체)
import org.springframework.security.crypto.password.PasswordEncoder;

// @Component: 이 클래스를 스프링 빈으로 등록
import org.springframework.stereotype.Component;

// @Transactional: DB 작업을 하나의 트랜잭션으로 묶음
import org.springframework.transaction.annotation.Transactional;

// ── @Component ───────────────────────────────────────────────
// 스프링 빈으로 등록하여 자동으로 run() 메서드가 실행되도록 합니다.
@Component

// ── @RequiredArgsConstructor ─────────────────────────────────
// final 필드에 대한 생성자를 자동 생성 → 의존성 주입(DI)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    // ── 필드 (의존성) ────────────────────────────────────────

    // MemberRepository: DB에서 모든 회원을 조회하기 위한 저장소
    private final MemberRepository memberRepository;

    // PasswordEncoder: 비밀번호를 BCrypt로 암호화하는 인코더
    // SecurityConfig에서 @Bean으로 등록한 BCryptPasswordEncoder가 주입됨
    private final PasswordEncoder passwordEncoder;

    // ── run 메서드 ───────────────────────────────────────────
    // 역할: Spring Boot 시작 완료 후 자동 실행되어, 평문 비밀번호를 BCrypt로 변환
    //
    // @Override: CommandLineRunner 인터페이스의 메서드를 구현
    // @Transactional: DB 변경(비밀번호 업데이트)을 트랜잭션으로 묶음
    //   → 중간에 오류 발생 시 모든 변경사항을 자동으로 되돌림(rollback)
    //
    @Override
    @Transactional
    public void run(String... args) {
        // data.sql의 INSERT로 생성된 모든 회원을 조회
        memberRepository.findAll().forEach(member -> {
            // 이미 BCrypt 해시가 아닌 경우에만 암호화 (평문 비밀번호)
            // BCrypt 해시는 "$2a$"로 시작하므로, 이것으로 판별합니다.
            if (!member.getPassword().startsWith("$2a$")) {
                // Member 엔티티의 encodePassword() 메서드로 비밀번호를 BCrypt 암호화
                member.encodePassword(passwordEncoder);
                // JPA의 더티 체킹(dirty checking)에 의해 자동으로 DB에 UPDATE됩니다.
                // → 별도로 save()를 호출하지 않아도 @Transactional 안에서 필드 변경 시 자동 반영
            }
        });
    }
}