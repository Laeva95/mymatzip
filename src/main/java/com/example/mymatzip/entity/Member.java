package com.example.mymatzip.entity; // 이 파일이 속한 패키지(폴더) 선언
// com.example.mymatzip.entity = entity 패키지 안에 있다는 뜻
// 같은 패키지 안의 Role.java를 import 없이 바로 사용할 수 있습니다.

// ──────────────────────────────────────────────────────────
// import (임포트) 선언 영역
// 다른 패키지에 있는 클래스를 이 파일에서 사용하기 위해 가져옵니다.
// ──────────────────────────────────────────────────────────

import jakarta.persistence.*;
// jakarta.persistence 패키지의 모든 클래스를 가져옵니다 (*는 "전부"라는 뜻).
// JPA(Java Persistence API) = 자바에서 DB를 쉽게 다루기 위한 도구 모음
// 이 안에 포함된 것들:
//   @Entity -- 이 클래스가 DB 테이블임을 알려주는 어노테이션
//   @Id -- 기본키(PK) 필드를 지정하는 어노테이션
//   @GeneratedValue -- 기본키 자동 생성 전략을 지정하는 어노테이션
//   @Column -- DB 컬럼의 속성(nullable, unique 등)을 지정하는 어노테이션
//   @Enumerated -- enum 값을 DB에 어떤 형태로 저장할지 지정하는 어노테이션
//   GenerationType -- 기본키 생성 전략의 종류 (IDENTITY, SEQUENCE 등)
//   EnumType -- enum 저장 방식 (STRING, ORDINAL)

import lombok.AccessLevel;
// Lombok(롬복) 라이브러리에서 AccessLevel(접근 수준)을 가져옵니다.
// AccessLevel은 접근 범위를 정하는 열거형입니다.
//   PUBLIC -- 모든 곳에서 접근 가능
//   PROTECTED -- 같은 패키지 + 자식 클래스에서만 접근 가능
//   PRIVATE -- 같은 클래스 안에서만 접근 가능
// 여기서는 @NoArgsConstructor의 access 속성에 PROTECTED를 전달하기 위해 사용

import lombok.Builder;
// Lombok에서 @Builder 어노테이션을 가져옵니다.
// @Builder는 "빌더 패턴"이라는 객체 생성 방식을 자동으로 만들어주는 기능

import lombok.Getter;
// Lombok에서 @Getter 어노테이션을 가져옵니다.
// @Getter를 클래스 위에 붙이면, 모든 필드의 getXxx() 메서드를 자동 생성

import lombok.NoArgsConstructor;
// Lombok에서 @NoArgsConstructor(노-아그스-컨스트럭터) 어노테이션을 가져옵니다.
// 파라미터(매개변수)가 없는 기본 생성자를 자동으로 만들어주는 기능
// 기본 생성자란? new Member() 처럼 아무 값도 넣지 않고 빈 객체를 만드는 것
// JPA가 DB에서 데이터를 가져와 객체를 만들 때 내부적으로 이 기본 생성자를 사용합니다.

import java.time.LocalDateTime;
// 자바의 날짜+시간 클래스를 가져옵니다.
// LocalDateTime = 날짜와 시간을 함께 저장하는 타입
// 예: 2026-03-13T14:30:00 = 2026년 3월 13일 오후 2시 30분
// 여기서는 회원 가입일(createdAt) 필드에 사용됩니다.

// ============================================================
// [Member.java] 회원 엔티티 -- DB의 member 테이블과 1:1 매핑되는 클래스
// ============================================================
//
// 역할: 회원 정보를 저장하는 DB 테이블을 자바 클래스로 표현한 것입니다.
//       이 클래스의 객체 하나 = DB member 테이블의 행(row) 하나 = 회원 1명
//
//
// 엔티티(Entity)란?
//   "이 자바 클래스 = DB 테이블 1개"라는 뜻입니다.
//   클래스의 필드 하나 = 테이블의 컬럼(열) 하나
//   클래스의 객체 하나 = 테이블의 행(row) 하나
//
//   DB 테이블 구조 (엑셀처럼 생긴 표):
//   | id | email           | password | nickname   | role  | created_at          |
//   |----|-----------------|----------|------------|-------|---------------------|
//   | 1  | admin@test.com  | 1234     | 관리자      | ADMIN | 2026-03-13 10:00:00 |
//   | 2  | user1@test.com  | 1234     | 맛집탐험가  | USER  | 2026-03-13 11:00:00 |
//
// 이 엔티티가 사용되는 곳들:
//   1. MemberService -- 회원가입, 로그인, 이메일/닉네임 중복 체크
//   2. MemberRepository -- DB에서 회원 조회 (findByEmail 등)
//   3. MemberApiController -- 회원가입/로그인 API
//   4. Restaurant 엔티티 -- 맛집 등록자 정보 (member 필드)
//   5. Review 엔티티 -- 리뷰 작성자 정보 (member 필드)
//   6. Wishlist 엔티티 -- 찜한 회원 정보 (member 필드)
//   7. 세션(Session) -- 로그인 후 세션에 Member 객체 저장
// ============================================================

// @Entity (엔티티): "이 클래스 = DB 테이블 1개" 라고 JPA에게 알려주는 어노테이션
//   이 어노테이션이 있어야 JPA가 이 클래스를 보고 DB 테이블을 자동으로 만들거나 연결합니다.
//   이 어노테이션이 없으면 그냥 평범한 자바 클래스이고, DB와 아무 관계도 없습니다.
@Entity

// @Getter (겟터): Lombok이 모든 필드의 getXxx() 메서드를 자동 생성해주는 어노테이션
//   자바에서는 필드를 private으로 숨기고, getter 메서드로만 값을 읽게 하는 것이 원칙입니다.
//   이 어노테이션 덕분에 아래의 모든 필드에 대해 getter가 자동 생성됩니다:
//     getId(), getEmail(), getPassword(), getNickname(), getRole(), getCreatedAt()
//   이걸 안 쓰면 6개의 getter 메서드를 직접 타이핑해야 합니다 -- Lombok이 이 수고를 덜어줌!
@Getter

// @NoArgsConstructor(access = AccessLevel.PROTECTED)
//   읽는 법: 노-아그스-컨스트럭터 (No Args Constructor = 매개변수 없는 생성자)
//
//   뜻: 매개변수(args)가 없는 기본 생성자를 만들되, 접근 수준을 protected로 제한합니다.
//
//   왜 필요한가?
//     JPA는 DB에서 데이터를 가져와 객체를 만들 때, 내부적으로 기본 생성자(new Member())를 사용합니다.
//     기본 생성자가 없으면 JPA가 객체를 만들 수 없어서 에러가 발생합니다!
//
//   왜 protected인가?
//     public으로 만들면 아무 곳에서나 new Member()로 빈 객체를 만들 수 있습니다.
//     빈 객체는 email, password 등이 null(비어있음)이므로 사용하면 오류가 생길 수 있습니다.
//     protected로 제한하면 JPA 내부에서만 사용 가능하고, 개발자는 @Builder로만 객체를 생성합니다.
//
//   Lombok이 자동으로 생성하는 코드:
//     protected Member() { }  ← 이걸 우리 대신 만들어줌
@NoArgsConstructor(access = AccessLevel.PROTECTED)

// public: 다른 파일(클래스)에서도 이 클래스를 사용할 수 있도록 공개
// class: 이것이 클래스(설계도)임을 선언하는 키워드
// Member: 클래스 이름. "회원"이라는 뜻. DB에서는 "member" 테이블과 매핑됨
public class Member {

    // ──────────────────────────────────────────────────────────
    // 필드(변수) 영역 -- DB 테이블의 각 컬럼(열)에 해당합니다
    // ──────────────────────────────────────────────────────────

    // @Id: 이 필드가 테이블의 기본키(PK = Primary Key)임을 JPA에게 알려줍니다.
    //   기본키란? DB 테이블에서 각 행(row)을 유일하게 구분하는 번호입니다.
    @Id

    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    //   읽는 법: 제너레이티드-밸류 (Generated Value = 자동 생성되는 값)
    //
    //   뜻: DB가 자동으로 기본키 번호를 매겨줍니다 (1, 2, 3, 4...)
    //   strategy = GenerationType.IDENTITY:
    //     DB의 AUTO_INCREMENT(자동 증가) 기능을 사용한다는 뜻
    //     첫 번째 회원 = 1, 두 번째 회원 = 2, 세 번째 회원 = 3 ...
    //   개발자가 id 값을 직접 넣을 필요가 없습니다!
    //
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    // private: 이 클래스 안에서만 직접 접근 가능 (외부에서는 getId()로 접근)
    // Long: 큰 정수를 저장하는 타입 (최대 약 922경). long의 객체 버전(wrapper)
    //   왜 int가 아니라 Long인가?
    //     int는 최대 약 21억이지만, Long은 훨씬 큰 수를 저장 가능
    //     또한 Long은 null(값 없음)을 허용하여, 아직 DB에 저장 안 된 객체를 표현 가능
    // id: 변수 이름. 회원의 고유 번호를 저장
    private Long id;

    // @Column(nullable = false, unique = true)
    //   @Column: DB 테이블의 컬럼(열) 속성을 세부적으로 설정하는 어노테이션
    //
    //   nullable = false:
    //     이 컬럼에 NULL(빈 값)을 넣을 수 없다는 뜻 → 반드시 값이 있어야 함!
    //
    //   unique = true:
    //     이 컬럼의 값은 테이블 전체에서 중복될 수 없다는 뜻
    //     같은 이메일로 두 번 가입하려고 하면 DB가 거부합니다!
    //
    //   [사용 화면] 로그인 페이지 -- 이메일을 입력하면 이 필드로 회원을 찾음
    //   [사용 화면] 회원가입 페이지 -- 이메일 중복 체크 시 이 필드를 검색
    @Column(nullable = false, unique = true)

    // String: 문자열(텍스트) 타입. 글자를 저장할 수 있는 자바 기본 타입
    // email: 변수 이름. 회원의 이메일 주소 (로그인 시 아이디로 사용)
    //   예: "hong@gmail.com", "admin@test.com"
    private String email;

    // @Column(nullable = false): 비밀번호는 필수값. 빈 값(NULL) 불가
    //   unique는 설정 안 함 → 같은 비밀번호를 쓰는 회원이 여러 명 있어도 됨
    //   [사용 화면] 로그인 페이지 -- 입력한 비밀번호와 DB의 비밀번호를 비교하여 인증
    //   [주의] 현재는 평문(암호화 안 함) 저장. 실제 서비스에서는 반드시 암호화 필요!
    @Column(nullable = false)

    // password: 변수 이름. 회원의 비밀번호를 저장
    //   예: "password123", "mySecret!"
    private String password;

    // @Column(nullable = false, unique = true): 닉네임도 필수 + 중복 불가
    //   nullable = false → 닉네임 없는 회원은 존재할 수 없음
    //   unique = true → 같은 닉네임을 쓰는 회원이 2명 이상 있을 수 없음
    //   [사용 화면] 리뷰 목록 -- "작성자: 맛집탐험가" 처럼 닉네임이 표시됨
    //   [사용 화면] 회원가입 -- 닉네임 중복 체크 시 이 필드를 검색
    @Column(nullable = false, unique = true)

    // nickname: 변수 이름. 회원의 닉네임(별명)을 저장
    //   다른 회원에게 보이는 이름. 실명 대신 사용
    //   예: "맛집탐험가", "먹방킹", "서울미식가"
    private String nickname;

    // @Enumerated(EnumType.STRING): enum 값을 DB에 문자열(STRING) 형태로 저장
    //   EnumType.STRING → DB에 "USER", "ADMIN" 글자 그대로 저장됨
    //   EnumType.ORDINAL → DB에 0, 1 같은 순번(숫자)으로 저장됨 (사용하지 않는 것을 권장!)
    //
    //   왜 STRING을 써야 하나?
    //     ORDINAL(순번)을 쓰면, 나중에 enum 순서가 바뀌었을 때 데이터가 꼬입니다.
    //     예: USER=0, ADMIN=1 인데, 나중에 GUEST를 맨 앞에 추가하면
    //         GUEST=0, USER=1, ADMIN=2가 되어 기존 데이터의 의미가 바뀌어버림!
    //     STRING을 쓰면 글자 그대로 저장되므로 순서가 바뀌어도 안전합니다.
    //
    //
    //   [사용 화면] 관리자 전용 기능 -- 맛집 삭제, 다른 사람 리뷰 삭제 등 권한 체크
    @Enumerated(EnumType.STRING)

    // @Column(nullable = false): 역할(role)은 필수값. 모든 회원은 반드시 역할이 있어야 함
    @Column(nullable = false)

    // Role: Role 열거형 타입 (이 패키지의 Role.java에서 정의)
    //   Role.USER 또는 Role.ADMIN 값만 가질 수 있음
    // role: 변수 이름. 이 회원의 권한 등급을 저장
    private Role role;

    // ──────────────────────────────────────────────────────────
    // 소셜 로그인(OAuth2) 관련 필드
    // ──────────────────────────────────────────────────────────

    // provider: 소셜 로그인 제공자 이름 (예: "google")
    //   일반 로그인 회원은 null, 소셜 로그인 회원만 값이 있음
    @Column
    private String provider;

    // providerId: 소셜 로그인 제공자가 부여한 고유 ID (예: "123456789")
    //   각 소셜 서비스에서 사용자를 식별하는 고유 번호
    @Column
    private String providerId;

    // @Column(nullable = false, updatable = false)
    //   nullable = false → 가입일은 필수값 (비어있을 수 없음)
    //   updatable = false → 한 번 DB에 저장되면 이 컬럼은 절대 수정할 수 없음!
    //     JPA가 UPDATE SQL을 실행할 때 이 컬럼을 아예 제외시킵니다.
    @Column(nullable = false, updatable = false)

    // LocalDateTime: 날짜 + 시간을 함께 저장하는 자바 타입
    //   예: 2026-03-13T14:30:00 (2026년 3월 13일 오후 2시 30분)
    // createdAt: 변수 이름. "생성된 시각"이라는 뜻. 회원 가입 시각을 저장
    //   DB 컬럼명: 자바의 camelCase(createdAt) → DB의 snake_case(created_at) 자동 변환
    private LocalDateTime createdAt;

    // ──────────────────────────────────────────────────────────
    // 생성자(Constructor) 영역
    // ──────────────────────────────────────────────────────────

    // @Builder (빌더): Lombok이 빌더 패턴 코드를 자동 생성해주는 어노테이션
    //
    //   빌더 패턴이란?
    //     객체를 만들 때 "주문서 작성" 방식을 사용하는 것입니다.
    //     각 항목을 하나씩 적고, 마지막에 "주문 완료!(build)" 하면 객체가 만들어집니다.
    //
    //   왜 빌더 패턴을 쓰나?
    //     일반 생성자: new Member("hong@mail.com", "1234", "홍길동", Role.USER)
    //       → 매개변수 순서를 외워야 하고, 순서가 바뀌면 오류 발생!
    //       → 어떤 값이 email이고 어떤 값이 password인지 헷갈림
    //     빌더 패턴: Member.builder()
    //                   .email("hong@mail.com")    ← 이메일 항목 작성
    //                   .password("1234")           ← 비밀번호 항목 작성
    //                   .nickname("홍길동")          ← 닉네임 항목 작성
    //                   .build()                    ← 주문 완료! 객체 생성!
    //       → 각 항목에 이름이 붙어있어 순서가 달라도 되고, 가독성이 높음!
    //
    //
    //   [참고] role을 안 넣으면 기본값 USER로 설정됩니다 (아래 삼항 연산자 참고)
    //   [참고] createdAt은 자동으로 현재 시간이 들어갑니다 (개발자가 넣을 필요 없음)
    @Builder

    // public: 다른 클래스에서도 이 생성자를 호출할 수 있음
    // Member: 생성자 이름 (클래스 이름과 동일해야 함)
    // 매개변수 4개:
    //   String email -- 회원 이메일 (로그인 아이디)
    //   String password -- 회원 비밀번호
    //   String nickname -- 회원 닉네임
    //   Role role -- 회원 역할 (USER 또는 ADMIN). null이면 기본값 USER
    public Member(String email, String password, String nickname, Role role) {
        this.email = email;
        // this.email: 이 객체(회원)의 email 필드 (위에서 선언한 멤버 변수)
        // email: 매개변수로 전달받은 이메일 값
        // this를 붙이는 이유: 필드명과 매개변수명이 같으므로 구분하기 위해
        // "이 회원의 이메일을 전달받은 이메일 값으로 설정해라"

        this.password = password;
        // "이 회원의 비밀번호를 전달받은 비밀번호 값으로 설정해라"

        this.nickname = nickname;
        // "이 회원의 닉네임을 전달받은 닉네임 값으로 설정해라"

        this.role = (role != null) ? role : Role.USER;
        // 삼항 연산자(조건 ? 참일때값 : 거짓일때값)를 사용한 기본값 설정
        //
        // 읽는 법:
        //   "role이 null이 아니면(값이 있으면) → 전달받은 role을 사용"
        //   "role이 null이면(값이 없으면) → Role.USER를 기본값으로 사용"
        //
        // 왜 이렇게 하나?
        //   회원가입 시 대부분 일반 사용자이므로, role을 안 넣으면 자동으로 USER가 됩니다.
        //   관리자 계정은 특별히 Role.ADMIN을 명시적으로 넣어야만 만들 수 있습니다.
        //

        this.createdAt = LocalDateTime.now();
        // LocalDateTime.now(): 현재 날짜+시간을 가져오는 메서드
        //   예: 지금이 2026년 3월 13일 오후 2시 30분이면 → 2026-03-13T14:30:00
        // 회원 객체가 만들어지는 순간 = 회원이 가입하는 순간이므로,
        // 가입일을 현재 시간으로 자동 설정합니다.
        // 개발자가 직접 날짜를 입력할 필요가 없어서 편리합니다!
    }

    // ──────────────────────────────────────────────────────────
    // 비밀번호 암호화 메서드
    // ──────────────────────────────────────────────────────────
    // 역할: 평문 비밀번호를 BCrypt로 암호화하여 저장합니다.
    // 사용처: DataInitializer에서 data.sql의 평문 비밀번호를 암호화할 때 사용
    // JPA 더티 체킹: @Transactional 안에서 이 메서드로 password를 변경하면
    //               JPA가 자동으로 UPDATE SQL을 실행합니다.
    public void encodePassword(org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.password = passwordEncoder.encode(this.password);
    }

    // ──────────────────────────────────────────────────────────
    // 역할(권한) 변경 메서드
    // ──────────────────────────────────────────────────────────
    // 역할: 관리자가 회원의 역할(USER ↔ ADMIN)을 변경할 때 사용
    // 사용처: AdminService.changeRole()에서 호출
    public void changeRole(Role role) {
        this.role = role;
    }

    // ──────────────────────────────────────────────────────────
    // 닉네임 변경 메서드
    // ──────────────────────────────────────────────────────────
    // 역할: 마이페이지에서 닉네임을 변경할 때 사용
    // 사용처: MemberService.updateNickname()에서 호출
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    // ──────────────────────────────────────────────────────────
    // 비밀번호 변경 메서드
    // ──────────────────────────────────────────────────────────
    // 역할: 마이페이지에서 비밀번호를 변경할 때 사용
    // 매개변수: encodedPassword — 이미 BCrypt로 암호화된 비밀번호
    // 사용처: MemberService.updatePassword()에서 호출
    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    // ── [추가] 소셜 계정 연결 ──────────────────────────────────
    // 소셜 로그인으로 가입하거나 기존 회원이 소셜 로그인을 처음 했을 때
    // 어떤 서비스(provider: "google")의 몇 번 회원(providerId)인지 저장합니다.
    // MemberRepository.findByProviderAndProviderId() 가 이 값으로 회원을 찾습니다.
    public void linkSocial(String provider, String providerId) {
        this.provider = provider;
        this.providerId = providerId;
    }
}