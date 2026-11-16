// ============================================================
// [MemberService.java] 회원 비즈니스 로직 서비스
// ============================================================
// 역할: 회원과 관련된 핵심 업무(회원가입, 로그인, 조회)를 처리합니다.
//       Controller로부터 요청을 받아 → Repository를 통해 DB를 조작합니다.
//
// MVC 아키텍처에서의 위치:
//   Controller (요청 접수) → ★Service (업무 처리)★ → Repository (DB 접근)
//
//
// 사용처:
//   - MemberController에서 회원가입, 로그인 요청 시 호출됩니다.
//   - 다른 Service에서 회원 정보가 필요할 때 findById()를 호출할 수 있습니다.
//
// @Transactional 설명:
//   트랜잭션 = "하나의 작업 단위" (중간에 실패하면 전부 취소)
//   예: 회원가입 중 닉네임 저장에 실패하면, 이메일 저장도 취소됨
//   readOnly=true (클래스 기본): 읽기 전용 → DB 잠금 최소화 → 성능 향상
//   @Transactional (메서드): 쓰기 작업 → readOnly 해제
// ============================================================

// 이 파일이 속한 패키지(폴더) 선언 — service 패키지에 위치함을 나타냄
package com.example.mymatzip.service;

// ─── import 영역 ─────────────────────────────────────────────
// LoginForm: 로그인 시 사용자가 입력한 이메일/비밀번호를 담는 DTO(데이터 전달 객체)
import com.example.mymatzip.dto.LoginForm;

// MemberSignupForm: 회원가입 시 사용자가 입력한 이메일/비밀번호/닉네임을 담는 DTO
import com.example.mymatzip.dto.MemberSignupForm;

// Member: 회원 정보를 담는 엔티티(Entity) — DB의 member 테이블과 1:1 대응
import com.example.mymatzip.entity.Member;

// Role: 회원의 역할(권한)을 나타내는 열거형 — USER(일반), ADMIN(관리자) 등
import com.example.mymatzip.entity.Role;

// MemberRepository: DB의 member 테이블에 접근하는 인터페이스 (CRUD 기능 제공)
import com.example.mymatzip.repository.MemberRepository;

// @RequiredArgsConstructor: final 필드에 대한 생성자를 자동 생성해주는 Lombok 어노테이션
// → 직접 생성자를 작성하지 않아도 의존성 주입(DI)이 자동으로 이루어짐
import lombok.RequiredArgsConstructor;

// PasswordEncoder: 비밀번호를 암호화(BCrypt)하고 비교하는 인터페이스
// SecurityConfig에서 BCryptPasswordEncoder를 @Bean으로 등록해두었으므로 자동 주입됩니다.
import org.springframework.security.crypto.password.PasswordEncoder;

// @Service: 이 클래스가 "서비스 계층"임을 Spring에게 알려주는 어노테이션
// → Spring이 자동으로 이 클래스의 객체(Bean)를 생성하고 관리함
import org.springframework.stereotype.Service;

// @Transactional: DB 작업을 하나의 트랜잭션(작업 단위)으로 묶어주는 어노테이션
// → 작업 도중 에러 발생 시 모든 변경사항을 자동으로 되돌림 (rollback)
import org.springframework.transaction.annotation.Transactional;

// ─── 클래스 선언부 ────────────────────────────────────────────

// @Service: Spring에게 "이 클래스는 비즈니스 로직을 담당하는 서비스입니다"라고 등록
//           → Spring 컨테이너가 이 클래스의 인스턴스를 자동 생성·관리함
@Service

// @RequiredArgsConstructor: 아래의 final 필드(memberRepository, passwordEncoder)를
//                           매개변수로 받는 생성자를 Lombok이 자동 생성해줌
//                           → 개발자가 직접 생성자를 작성하지 않아도 됨
//                           → Spring이 생성자를 통해 자동 주입(DI)
@RequiredArgsConstructor

// @Transactional(readOnly = true): 이 클래스의 모든 메서드에 기본적으로 "읽기 전용" 트랜잭션 적용
//   - readOnly = true란? → DB에서 데이터를 "읽기만" 하겠다는 의미
//   - 장점: DB가 불필요한 잠금(Lock)을 걸지 않아 성능이 향상됨
//   - 쓰기가 필요한 메서드에는 별도로 @Transactional을 붙여 readOnly를 해제함
@Transactional(readOnly = true)
public class MemberService {

    // ─── 필드(의존성) 영역 ────────────────────────────────────

    // memberRepository: DB의 member 테이블에 접근하기 위한 저장소 객체
    // final 키워드: 한 번 주입된 후 변경 불가 → 안전성 보장
    // Spring이 자동으로 MemberRepository 구현체를 주입해줌 (의존성 주입, DI)
    private final MemberRepository memberRepository;

    // ── [Security] 비밀번호 암호화 인코더 ─────────────────────
    // passwordEncoder: 비밀번호를 BCrypt 알고리즘으로 암호화하고, 비교하는 도구
    // SecurityConfig에서 @Bean으로 등록한 BCryptPasswordEncoder가 자동 주입됩니다.
    //
    // BCrypt란?
    //   비밀번호를 "해시(hash)"라는 되돌릴 수 없는 암호문으로 변환하는 알고리즘입니다.
    //   예: "1234" → "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl4in9GGvri"
    //   같은 "1234"를 두 번 암호화해도 매번 다른 결과가 나옵니다 (솔트salt 때문).
    //   하지만 matches() 메서드로 원본과 해시가 일치하는지는 확인할 수 있습니다.
    //
    private final PasswordEncoder passwordEncoder;

    // ─── 회원가입 메서드 ──────────────────────────────────────

    // @Transactional: 이 메서드는 DB에 데이터를 "쓰는" 작업이므로
    //                 클래스 레벨의 readOnly=true를 덮어씀 (쓰기 가능 트랜잭션)
    //                 → 메서드 실행 중 오류 발생 시 모든 DB 변경이 자동 취소(rollback)됨
    @Transactional
    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: signup (회원가입)                                          │
    // │ 매개변수: MemberSignupForm form — 사용자가 입력한 가입 정보            │
    // │          (이메일, 비밀번호, 닉네임이 담겨 있음)                        │
    // │ 리턴값:  Member — 가입이 완료된 회원 엔티티 객체                       │
    // │ 동작 흐름:                                                          │
    // │   1단계: 이메일 중복 확인 → 이미 존재하면 예외(에러) 발생               │
    // │   2단계: 닉네임 중복 확인 → 이미 존재하면 예외(에러) 발생               │
    // │   3단계: Member 객체 생성 (빌더 패턴 사용) + 비밀번호 BCrypt 암호화     │
    // │   4단계: DB에 저장 후 저장된 Member 객체 반환                         │
    // └─────────────────────────────────────────────────────────────────────┘
    public Member signup(MemberSignupForm form) {

        // [1단계] 이메일 중복 확인
        // memberRepository.existsByEmail(): DB에서 해당 이메일이 이미 존재하는지 확인 (true/false)
        // form.getEmail(): 사용자가 입력한 이메일 주소를 가져옴
        // 만약 이미 존재하면(true) → 예외를 던져서 회원가입을 중단시킴
        if (memberRepository.existsByEmail(form.getEmail())) {
            // IllegalArgumentException: "잘못된 입력값" 예외 → Controller에서 에러 메시지로 사용자에게 표시
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // [2단계] 닉네임 중복 확인
        // memberRepository.existsByNickname(): DB에서 해당 닉네임이 이미 존재하는지 확인 (true/false)
        // form.getNickname(): 사용자가 입력한 닉네임을 가져옴
        // 만약 이미 존재하면(true) → 예외를 던져서 회원가입을 중단시킴
        if (memberRepository.existsByNickname(form.getNickname())) {
            // 닉네임이 중복될 경우 예외 발생 → 사용자에게 "이미 사용 중인 닉네임" 안내
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // [3단계] Member 엔티티 객체 생성 (빌더 패턴 사용)
        // 빌더 패턴이란? → new Member(이메일, 비밀번호, ...) 대신
        //                  .email("값").password("값") 형태로 가독성 좋게 객체를 생성하는 방법
        // Member.builder(): 빌더 패턴의 시작
        Member member = Member.builder()
                // .email(): 사용자가 입력한 이메일을 Member 객체에 설정
                .email(form.getEmail())
                // ── [Security] 비밀번호를 BCrypt로 암호화하여 저장 ──
                // passwordEncoder.encode(): 평문 비밀번호를 BCrypt 해시로 변환
                // 이전: .password(form.getPassword())  ← 평문 저장 (위험!)
                // 현재: .password(passwordEncoder.encode(form.getPassword()))  ← 암호화 저장 (안전!)
                //
                .password(passwordEncoder.encode(form.getPassword()))
                // .nickname(): 사용자가 입력한 닉네임을 Member 객체에 설정
                .nickname(form.getNickname())
                // .role(): 회원의 역할을 USER(일반 사용자)로 설정
                // 모든 신규 가입자는 일반 사용자(USER) 권한으로 시작
                .role(Role.USER)
                // .build(): 위에서 설정한 값들로 최종 Member 객체를 생성
                .build();

        // [4단계] DB에 회원 정보 저장 후 반환
        // memberRepository.save(member): Member 객체를 DB의 member 테이블에 INSERT(삽입)
        // 저장 후 DB가 자동 생성한 ID(고유번호)가 포함된 Member 객체를 반환
        return memberRepository.save(member);
    }

    // ─── 로그인 메서드 ────────────────────────────────────────

    // @Transactional이 없음 → 클래스 레벨의 @Transactional(readOnly = true)가 적용됨
    // → 이 메서드는 DB에서 데이터를 읽기만 하므로 readOnly로 충분함
    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: login (로그인)                                             │
    // │ 매개변수: LoginForm form — 사용자가 입력한 로그인 정보                  │
    // │          (이메일, 비밀번호가 담겨 있음)                                │
    // │ 리턴값:  Member — 로그인에 성공한 회원 엔티티 객체                      │
    // │ 동작 흐름:                                                          │
    // │   1단계: 이메일로 회원 조회 → 없으면 예외(에러) 발생                    │
    // │   2단계: BCrypt로 비밀번호 일치 확인 → 틀리면 예외(에러) 발생           │
    // │   3단계: 로그인 성공 → 회원 정보 반환                                 │
    // └─────────────────────────────────────────────────────────────────────┘
    public Member login(LoginForm form) {

        // [1단계] 이메일로 회원 조회
        // memberRepository.findByEmail(): DB에서 해당 이메일을 가진 회원을 검색
        // → 결과는 Optional<Member> 타입 (값이 있을 수도, 없을 수도 있음)
        // .orElseThrow(): 값이 없으면(회원이 없으면) 예외를 던짐
        // → 값이 있으면 Member 객체를 꺼내서 member 변수에 저장
        Member member = memberRepository.findByEmail(form.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 이메일입니다."));

        // [2단계] BCrypt로 비밀번호 일치 확인
        // ── [Security] passwordEncoder.matches()로 비밀번호 비교 ──
        // 이전: !member.getPassword().equals(form.getPassword())  ← 평문 비교 (위험!)
        // 현재: !passwordEncoder.matches(form.getPassword(), member.getPassword())  ← BCrypt 비교 (안전!)
        //
        // passwordEncoder.matches(원본 비밀번호, 암호화된 해시):
        //   원본 비밀번호를 BCrypt로 변환한 후, DB에 저장된 해시와 비교합니다.
        //   일치하면 true, 불일치하면 false를 반환합니다.
        //
        if (!passwordEncoder.matches(form.getPassword(), member.getPassword())) {
            // 비밀번호가 틀리면 예외 발생 → Controller에서 에러 메시지를 사용자에게 보여줌
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // [3단계] 로그인 성공 → 회원 정보 반환
        // 이메일도 존재하고 비밀번호도 일치했으므로 로그인 성공
        // 조회한 Member 객체를 Controller에게 돌려줌
        // → Controller는 이 정보를 SecurityContext에 저장하여 로그인 상태를 유지
        return member;
    }

    // ─── 회원 ID로 조회 메서드 ─────────────────────────────────

    // @Transactional이 없음 → 클래스 레벨의 readOnly = true 적용
    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: findById (회원 ID로 조회)                                   │
    // │ 매개변수: Long id — 조회할 회원의 고유번호 (DB의 Primary Key)           │
    // │ 리턴값:  Member — 조회된 회원 엔티티 객체                              │
    // │ 동작 흐름:                                                          │
    // │   1단계: ID로 회원 조회 → 없으면 예외(에러) 발생                       │
    // │ 사용처: JWT 토큰에서 추출한 회원 ID로 전체 회원 정보를 가져올 때 사용     │
    // └─────────────────────────────────────────────────────────────────────┘
    public Member findById(Long id) {

        // memberRepository.findById(id): DB에서 해당 ID를 가진 회원을 검색
        // → JPA가 기본 제공하는 메서드 (별도 쿼리 작성 불필요)
        // .orElseThrow(): 회원이 존재하지 않으면 예외 발생
        // → 회원이 존재하면 Member 객체를 반환
        return memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
    }

    // ─── 닉네임 변경 메서드 ──────────────────────────────────────

    // @Transactional: DB에 데이터를 "수정"하므로 쓰기 가능 트랜잭션 필요
    @Transactional
    public void updateNickname(Long id, String newNickname) {
        // 닉네임 중복 확인
        if (memberRepository.existsByNickname(newNickname)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }
        // 회원 조회 후 닉네임 변경 (JPA 더티 체킹으로 자동 UPDATE)
        Member member = findById(id);
        member.updateNickname(newNickname);
    }

    // ─── 비밀번호 변경 메서드 ────────────────────────────────────

    // @Transactional: DB에 데이터를 "수정"하므로 쓰기 가능 트랜잭션 필요
    @Transactional
    public void updatePassword(Long id, String encodedPassword) {
        // 회원 조회 후 비밀번호 변경 (JPA 더티 체킹으로 자동 UPDATE)
        Member member = findById(id);
        member.updatePassword(encodedPassword);
    }
}