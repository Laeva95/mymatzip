package com.example.mymatzip.entity; // 이 파일이 속한 패키지(폴더) 선언
// com.example.mymatzip.entity = entity 패키지 안에 있다는 뜻
// 같은 패키지 안의 Category.java, Member.java를 import 없이 바로 사용 가능
// [추가]2026.11.11권성애 Source.java(출처 enum)도 같은 패키지라 import 없이 사용

// ──────────────────────────────────────────────────────────
// import (임포트) 선언 영역
// 다른 패키지에 있는 클래스를 이 파일에서 사용하기 위해 가져옵니다.
// ──────────────────────────────────────────────────────────

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
// JPA(Java Persistence API) 관련 어노테이션을 모두 가져옵니다.
// 포함된 것들:
//   @Entity -- 이 클래스가 DB 테이블임을 선언
//   @Id -- 기본키(PK) 필드 지정
//   @GeneratedValue -- 기본키 자동 생성 전략
//   @Column -- 컬럼 속성 설정 (nullable, unique, length 등)
//   @Enumerated -- enum 저장 방식 (STRING / ORDINAL)
//   @ManyToOne -- 다대일 관계 설정 (여러 맛집 → 한 명의 등록자)
//   @JoinColumn -- 외래키(FK) 컬럼명 설정
//   FetchType -- 데이터 로딩 전략 (LAZY / EAGER)
//   GenerationType, EnumType -- 각종 전략 상수
//   [추가]2026.11.11권성애 @Table, @UniqueConstraint -- 테이블 이름 · 유니크 제약(이름+주소) 설정

import lombok.AccessLevel;
// Lombok의 접근 수준 열거형. @NoArgsConstructor에서 PROTECTED 수준을 지정하기 위해 사용
// PROTECTED로 설정하면 JPA만 내부적으로 사용 가능, 개발자는 @Builder로만 객체 생성

import lombok.Builder;
// Lombok의 빌더 패턴 자동 생성 어노테이션
// Restaurant.builder().name("김치찌개명가").category(Category.KOREAN).build()
// 처럼 "주문서 작성" 방식으로 맛집 객체를 만들 수 있게 해줌

import lombok.Getter;
// Lombok의 getter 자동 생성 어노테이션
// 모든 필드의 getXxx() 메서드를 자동으로 만들어줌
// 예: getName(), getCategory(), getAddress() 등

import lombok.NoArgsConstructor;
// Lombok의 기본 생성자 자동 생성 어노테이션
// JPA가 DB 데이터를 객체로 변환할 때 필요한 빈 생성자를 만들어줌

import java.time.LocalDateTime;
// 날짜+시간을 저장하는 자바 표준 클래스
// 맛집 등록일(createdAt) 필드에 사용
// 예: 2026-03-13T14:30:00 = 2026년 3월 13일 오후 2시 30분

// ============================================================
// [Restaurant.java] 맛집 엔티티 -- DB의 restaurant 테이블과 1:1 매핑되는 클래스
// ============================================================
//
// 역할: 맛집 정보를 저장하는 DB 테이블을 자바 클래스로 표현한 것입니다.
//       이 클래스의 객체 하나 = DB restaurant 테이블의 행(row) 하나 = 맛집 1곳
//
//
// DB 테이블 구조:
//   | id | name         | category | address        | phone        | description      | member_id | created_at          |
//   |----|--------------|----------|----------------|--------------|------------------|-----------|---------------------|
//   | 1  | 김치찌개명가  | KOREAN   | 서울시 강남구   | 02-1234-5678 | 30년 전통 맛집    | 1         | 2026-03-13 10:00:00 |
//   | 2  | 스시오마카세  | JAPANESE | 서울시 종로구   | null         | null             | 2         | 2026-03-13 11:00:00 |
//
// [추가]2026.11.11권성애 공공 API 배치 수집을 위해 바뀐 점
//   1. 컬럼 추가: representative_menu(대표메뉴), business_hours(영업시간), lat · lng(위도 · 경도), source(출처)
//   2. description: varchar(500) → TEXT (공공 API 소개글이 500자를 넘을 수 있음. 회원 입력은 폼에서 500자 제한)
//   3. 유니크 제약: (name, address) — 같은 이름 + 같은 주소의 맛집은 한 번만 저장
//   ※ 테이블 반영: ddl-auto=create 1회 실행 → 이후 update + sql.init.mode=never 로 전환
//
// 관계(Relationship):
//   Member(회원) 1명 → Restaurant(맛집) 여러 개 등록 가능 (1:N 관계)
//   [추가]2026.11.11권성애 공공 API로 수집한 맛집의 등록자 = 배치를 실행한 관리자
//
// 이 엔티티가 사용되는 곳들:
//   1. RestaurantService -- 맛집 등록, 수정, 삭제, 조회 비즈니스 로직
//   2. RestaurantRepository -- DB에서 맛집 조회 (findByCategory, 검색 등)
//   3. RestaurantApiController -- 맛집 CRUD(생성/조회/수정/삭제) API
//   4. RestaurantViewController -- 맛집 목록/상세 화면 렌더링
//   5. Review 엔티티 -- 리뷰가 달린 맛집 정보 (restaurant 필드)
//   6. Wishlist 엔티티 -- 찜된 맛집 정보 (restaurant 필드)
//   7. RestaurantResponse (DTO) -- 맛집 정보를 화면에 전달할 때 사용
//   8. [추가]2026.11.11권성애 PublicRestaurantImportService -- 공공 API 맛집 배치 수집
// ============================================================

// @Entity: 이 클래스 = DB의 restaurant 테이블
//   JPA가 이 클래스를 보고 DB 테이블을 자동으로 만들거나 연결합니다.
//   테이블명: 클래스명(Restaurant) → 소문자 "restaurant"로 자동 변환
@Entity

// [추가]2026.11.11권성애 @Table(uniqueConstraints = ...): 이름 + 주소 유니크 제약
//   같은 이름과 같은 주소를 가진 행을 DB가 두 번 저장하지 못하게 막습니다.
//   배치는 저장 전에 existsByNameAndAddress()로 먼저 검사하고, 이 제약은 최종 안전장치입니다.
//   회원 등록에서 중복이면 DataIntegrityViolationException → RestaurantService에서 안내 메시지로 바꿔야 합니다.
//   주의: 기존 데이터에 같은 이름+주소가 이미 있으면 제약 생성이 실패합니다.
@Table(
        name = "restaurant",
        uniqueConstraints = @UniqueConstraint(name = "uk_restaurant_name_address", columnNames = {"name", "address"})
)

// @Getter: 모든 필드의 getXxx() 메서드를 Lombok이 자동 생성
//   getId(), getName(), getCategory(), getAddress(), getPhone(),
//   getDescription(), getMember(), getCreatedAt() -- 총 8개 자동 생성
//   [추가]2026.11.11권성애 getRepresentativeMenu(), getBusinessHours(), getLat(), getLng(), getSource()도 자동 생성
@Getter

// @NoArgsConstructor(access = AccessLevel.PROTECTED)
//   JPA 전용 기본 생성자. protected로 제한하여 외부에서 빈 맛집 객체 생성 방지
//   개발자는 아래의 @Builder를 통해서만 Restaurant 객체를 만들 수 있음
@NoArgsConstructor(access = AccessLevel.PROTECTED)

// public class Restaurant: 맛집 정보를 담는 공개 클래스
public class Restaurant {

    // ──────────────────────────────────────────────────────────
    // 필드(변수) 영역 -- DB 테이블의 각 컬럼(열)에 해당합니다
    // ──────────────────────────────────────────────────────────

    // @Id: 기본키(PK) 필드. 맛집을 유일하게 구분하는 번호
    //   모든 엔티티에는 반드시 @Id가 있어야 합니다.
    @Id

    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    //   DB가 자동으로 번호를 매겨줍니다 (1, 2, 3, 4 ...)
    //   AUTO_INCREMENT 기능 사용. 맛집을 등록할 때마다 번호가 1씩 증가
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    // Long id: 맛집의 고유 번호 (기본키)
    //   Long 타입 → null 허용 + 큰 수 저장 가능
    //   DB에 아직 저장 안 된 새 맛집은 id가 null일 수 있음
    private Long id;

    // @Column(nullable = false): 가게 이름은 필수값 (빈 값 불가)
    //   이름 없는 맛집은 존재할 수 없으므로 반드시 입력해야 합니다.
    //   [사용 화면] 맛집 목록 페이지 -- 맛집 카드에 가게 이름 표시
    //   [사용 화면] 맛집 상세 페이지 -- 제목으로 가게 이름 표시
    //   [사용 화면] 검색 결과 -- 검색된 맛집의 이름 표시
    @Column(nullable = false)

    // String name: 가게 이름을 저장하는 필드
    //   예: "김치찌개 명가", "스시 오마카세", "파스타 팩토리"
    private String name;

    // @Enumerated(EnumType.STRING): Category enum을 문자열로 DB에 저장
    //   DB에 "KOREAN", "CHINESE", "JAPANESE" 등 영어 대문자로 저장됨
    //   화면에 표시할 때는 Category.getDisplayName()으로 "한식", "중식" 등 한글을 가져옴
    @Enumerated(EnumType.STRING)

    // @Column(nullable = false): 카테고리는 필수값. 카테고리 없는 맛집은 불가
    //   [사용 화면] 맛집 등록 폼 -- 카테고리 드롭다운(선택 목록)에서 하나를 선택
    //   [사용 화면] 맛집 목록 -- 카테고리 탭으로 필터링 (한식만 보기, 일식만 보기 등)
    @Column(nullable = false)

    // Category category: 맛집의 음식 카테고리 (Category.java에서 정의한 enum 타입)
    //   가능한 값: KOREAN(한식), CHINESE(중식), JAPANESE(일식), WESTERN(양식), CAFE(카페), ETC(기타)
    //   [추가]2026.11.11권성애 SEAFOOD(해산물·회), MEAT(고기·구이), SOUP(국밥·탕) 추가
    private Category category;

    // @Column(nullable = false): 주소는 필수값. 주소 없는 맛집은 불가
    //   [사용 화면] 맛집 상세 페이지 -- "주소: 서울시 강남구 역삼동 ..." 표시
    @Column(nullable = false)

    // String address: 가게 주소를 저장하는 필드
    //   예: "서울시 강남구 역삼로 123", "부산시 해운대구 해운대로 456"
    //   [추가]2026.11.11권성애 공공 API 데이터는 "부산광역시 "로 시작하도록 통일해서 저장
    //     (화면 표시 · 이름+주소 유니크 판단 · 카카오 주소 검색 정확도)
    private String address;

    // phone 필드에는 @Column 어노테이션이 없습니다.
    //   @Column이 없으면 기본 설정이 적용됩니다:
    //     nullable = true (빈 값 허용) → 전화번호는 선택 사항!
    //     unique = false (중복 허용)
    //   [사용 화면] 맛집 상세 페이지 -- 전화번호 표시 (없으면 해당 영역 안 보임)
    //   [사용 화면] 맛집 등록 폼 -- 전화번호 입력란 (필수 아님)

    // String phone: 가게 전화번호를 저장하는 필드 (선택값, null 가능)
    //   예: "02-1234-5678", "010-9876-5432", null(미입력)
    private String phone;

    // @Column(length = 500): 이 컬럼에 저장할 수 있는 최대 문자 수 = 500자
    //   기본값은 255자이지만, 가게 소개글은 조금 더 길 수 있으므로 500자로 늘림
    //   nullable 설정 없음 → 기본값 true → 비워둬도 됨 (선택값)
    //   [사용 화면] 맛집 상세 페이지 -- 가게 소개글 영역
    //   [사용 화면] 맛집 등록 폼 -- 소개글 텍스트 영역 (textarea)
    // [추가]2026.11.11권성애 (수정: @Column(length = 500) → @Column(columnDefinition = "TEXT"))
    //   공공 API 소개글(ITEMCNTNTS)이 500자를 넘을 수 있어서 자르지 않고 저장하도록 TEXT로 변경.
    //   회원이 직접 입력할 때는 RestaurantForm @Size(max = 500) + 화면 JS에서 500자로 제한.
    @Column(columnDefinition = "TEXT")

    // String description: 가게 소개글/설명을 저장하는 필드 (선택값, null 가능)
    //   예: "30년 전통 김치찌개 전문점. 직접 담근 김치로 요리합니다."
    //   예: null (미입력)
    private String description;

    // [Phase 7] 맛집 대표 이미지 URL (선택값, null 가능)
    // 이미지를 업로드하면 이 필드에 파일 경로가 저장됩니다.
    // 예: "/uploads/a1b2c3d4-5678-9012-abcd-e5f6g7h8i9j0.jpg"
    // 예: null (이미지를 업로드하지 않은 경우)
    //   [사용 화면] 맛집 목록 카드 -- 대표 이미지 표시
    //   [사용 화면] 맛집 상세 페이지 -- 상단에 대표 이미지 표시
    // [추가]2026.11.11권성애 저장되는 값의 종류
    //   회원 업로드(local): "/uploads/UUID.jpg" / 회원 업로드(s3): "/api/files/UUID.jpg"
    //   공공 API(부산맛집): "https://www.visitbusan.net/uploadImgs/..." 외부 주소 그대로
    //   null이면 화면에서 카테고리 기본 이미지(Category.getDefaultImageUrl()), 깨지면 onerror로 같은 이미지
    private String imageUrl;

    // [추가]2026.11.11권성애 대표 메뉴 (선택값, null 가능)
    //   예: "돌솥곤드레정식, 단호박오리훈제"
    //   공공 API: 부산맛집 RPRSNTV_MENU / 모범음식점 menu
    @Column(length = 500)
    private String representativeMenu;

    // [추가]2026.11.11권성애 영업시간 (선택값, null 가능)
    //   줄바꿈이 들어 있음 → 화면 CSS white-space: pre-line;
    //   공공 API: 부산맛집 USAGE_DAY_WEEK_AND_TIME (모범음식점은 없음)
    @Column(length = 500)
    private String businessHours;

    // [추가]2026.11.11권성애 위도 · 경도 (지도 마커, 길찾기, 내 주변 맛집)
    //   공공 API: 응답 값 그대로 / 회원 등록: 카카오 지도 SDK Geocoder로 주소 → 좌표
    //   지금 등록 폼에 좌표 입력이 없어서 null 허용
    //   (카카오 지오코딩 JS를 붙인 뒤 RestaurantForm에 @NotNull 추가 예정)
    private Double lat;
    private Double lng;

    // [추가]2026.11.11권성애 출처 — 회원 직접 등록인지, 어느 공공 API인지
    //   폼에서 받지 않고 서버가 지정합니다 (개발자 도구로 조작 방지). 값이 없으면 생성자에서 MANUAL.
    //   columnDefinition = "varchar(40)": MySQL enum 타입으로 만들지 않기 위해 지정
    //     → 나중에 Source에 값을 추가해도 ALTER TABLE 없이 저장 가능
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(40)")
    private Source source;

    // ──────────────────────────────────────────────────────────
    // 관계(Relationship) 필드 -- 다른 엔티티와의 연결을 표현합니다
    // ──────────────────────────────────────────────────────────

    // @ManyToOne(fetch = FetchType.LAZY)
    //   읽는 법: 매니-투-원 (Many To One = 다대일)
    //
    //   뜻: "맛집 여러 개(Many)가 회원 1명(One)에게 속한다"
    //   다시 말해, 회원 1명이 맛집을 여러 개 등록할 수 있다는 관계입니다.
    //
    //   fetch = FetchType.LAZY (레이지 = 게으른):
    //     맛집 정보를 DB에서 가져올 때, 등록자(Member) 정보는 즉시 가져오지 않습니다.
    //     실제로 member 데이터가 필요한 순간(예: getMember() 호출)에야 비로소 DB를 조회합니다.
    //
    //   왜 Restaurant 쪽에 @ManyToOne을 달까?
    //     "여러(Many) 쪽"이 외래키(FK)를 가지는 것이 DB 설계 원칙입니다.
    //     restaurant 테이블에 member_id 컬럼이 있으므로, Restaurant가 관계의 주인입니다.
    @ManyToOne(fetch = FetchType.LAZY)

    // @JoinColumn(name = "member_id", nullable = false)
    //   읽는 법: 조인-컬럼 (Join Column = 연결 컬럼)
    //
    //   뜻: DB 테이블에서 이 관계를 저장하는 컬럼의 이름을 "member_id"로 지정합니다.
    //   restaurant 테이블에 "member_id"라는 컬럼이 만들어지고,
    //   여기에 맛집을 등록한 회원의 id(기본키) 값이 저장됩니다.
    //
    //   예: 회원 id=2인 "맛집탐험가"가 맛집을 등록하면,
    //       restaurant 테이블의 member_id 컬럼에 2가 저장됩니다.
    //
    //   nullable = false → 등록자 없는 맛집은 존재할 수 없음! (필수 관계)
    //     누군가 반드시 등록해야 맛집이 DB에 저장될 수 있습니다.
    //
    //
    //   [사용 화면] 맛집 상세 페이지 -- "등록자: 맛집탐험가" 표시
    //   [사용 화면] 맛집 수정/삭제 -- 본인이 등록한 맛집인지 확인 (member_id 비교)
    // [수정] 회원이 삭제되면 DB 가 이 행도 함께 지웁니다 (외래 키 ON DELETE CASCADE).
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "member_id", nullable = false)

    // Member member: 이 맛집을 등록한 회원 객체를 저장하는 필드
    //   Member 타입 → Member.java 엔티티의 객체
    //   이 필드를 통해 맛집에서 등록자의 모든 정보(이메일, 닉네임 등)에 접근 가능
    //   예: restaurant.getMember().getNickname() → "맛집탐험가"
    private Member member;

    // @Column(nullable = false, updatable = false)
    //   nullable = false → 등록일은 필수값
    //   updatable = false → 한 번 저장되면 수정 불가 (등록일은 바뀔 이유가 없음)
    //   [사용 화면] 맛집 목록 -- "최근 등록된 맛집" 정렬 기준으로 사용
    //   [사용 화면] 맛집 상세 -- "등록일: 2026-03-13" 표시
    @Column(nullable = false, updatable = false)

    // LocalDateTime createdAt: 맛집 등록 날짜+시간
    //   예: 2026-03-13T14:30:00
    //   DB 컬럼명: created_at (자바의 camelCase → DB의 snake_case 자동 변환)
    private LocalDateTime createdAt;

    // [추가]2026.11.11권성애 소프트 삭제 시각 — null = 정상, 값 있음 = 삭제됨 (컬럼 이름은 자동으로 deleted_at, DATETIME(6) NULL)
    //   맛집 행은 지우지 않고 이 값만 기록 → 리뷰 · 댓글 · 찜의 외래키가 그대로 유지됨
    //   목록 · 검색 · 상세는 RestaurantRepository의 ...DeletedAtIsNull 메서드로 삭제된 맛집을 뺌
    private LocalDateTime deletedAt;

    // ══════════════════════════════════════════════════════════
    // [팀 프로젝트] AI 리뷰 요약 캐시 (C 리뷰 담당)
    // ══════════════════════════════════════════════════════════
    // 무료 AI 모델은 하루 호출 횟수가 정해져 있어서, 한 번 만든 요약을 DB 에 저장해 두고 다시 보여 준다.
    // @Lob + @Column(columnDefinition = "TEXT"): Hibernate 가 CREATE TABLE 할 때 ai_summary 컬럼을
    //   길이 제한이 없는 TEXT 타입으로 만든다. (기본 VARCHAR(255) 로는 요약 글이 잘린다)
    @Lob
    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    // 요약을 만든 시각. 필드 이름 aiSummaryAt 은 Hibernate 의 이름 변환 규칙(CamelCaseToUnderscoresNamingStrategy)에 의해
    // DB 컬럼 ai_summary_at (TIMESTAMP 타입) 으로 만들어진다.
    private LocalDateTime aiSummaryAt;

    // ──────────────────────────────────────────────────────────
    // 생성자(Constructor) 영역
    // ──────────────────────────────────────────────────────────

    // @Builder: Lombok이 빌더 패턴 코드를 자동 생성
    //   사용 예시:
    //     Restaurant.builder()
    //         .name("김치찌개 명가")            // 가게 이름 설정
    //         .category(Category.KOREAN)        // 카테고리: 한식
    //         .address("서울시 강남구 역삼로 123") // 주소 설정
    //         .phone("02-1234-5678")            // 전화번호 설정 (선택)
    //         .description("30년 전통 맛집")     // 소개글 설정 (선택)
    //         .member(loginMember)              // 등록자(로그인한 회원) 설정
    //         .build();                         // 객체 생성 완료!
    //
    //   [참고] phone, description은 선택값이므로 빼고 .build()해도 됩니다.
    //   [참고] createdAt은 자동으로 현재 시간이 들어갑니다.
    //   [참고] id는 DB가 자동 생성하므로 넣지 않습니다.
    //   [추가]2026.11.11권성애 [참고] 새 값(대표메뉴 · 영업시간 · lat · lng · source)은 빼고 .build()해도 됩니다.
    //     → 기존 RestaurantService.save() 코드를 고치지 않아도 그대로 동작 (새 값은 null, source는 MANUAL)
    //   [추가]2026.11.11권성애 공공 API 배치에서는:
    //     Restaurant.builder()....representativeMenu(..).businessHours(..).lat(..).lng(..)
    //         .source(Source.PUBLIC_API_BUSAN_FOOD).member(admin).build();
    @Builder

    // 매개변수 5개:
    //   String name -- 가게 이름 (필수)
    //   Category category -- 카테고리 (필수)
    //   String address -- 주소 (필수)
    //   String phone -- 전화번호 (선택, null 가능)
    //   String description -- 소개글 (선택, null 가능)
    //   Member member -- 등록자 회원 객체 (필수)
    // 매개변수 7개:
    //   String name -- 가게 이름 (필수)
    //   Category category -- 카테고리 (필수)
    //   String address -- 주소 (필수)
    //   String phone -- 전화번호 (선택, null 가능)
    //   String description -- 소개글 (선택, null 가능)
    //   Member member -- 등록자 회원 객체 (필수)
    //   String imageUrl -- [Phase 7] 대표 이미지 URL (선택, null 가능)
    // [추가]2026.11.11권성애 (수정: 매개변수 7개 → 12개, 아래 5개 추가)
    //   String representativeMenu -- 대표 메뉴 (선택, null 가능)
    //   String businessHours -- 영업시간 (선택, null 가능)
    //   Double lat -- 위도 (선택, null 가능)
    //   Double lng -- 경도 (선택, null 가능)
    //   Source source -- 출처 (null이면 MANUAL)
    public Restaurant(String name, Category category, String address,
                      String phone, String description, Member member,
                      String imageUrl,
                      String representativeMenu, String businessHours,   // [추가]2026.11.11권성애
                      Double lat, Double lng, Source source) {           // [추가]2026.11.11권성애
        this.name = name;
        // "이 맛집의 이름을 전달받은 이름으로 설정해라"

        this.category = category;
        // "이 맛집의 카테고리를 전달받은 카테고리로 설정해라"
        // 예: Category.KOREAN, Category.JAPANESE 등

        this.address = address;
        // "이 맛집의 주소를 전달받은 주소로 설정해라"

        this.phone = phone;
        // "이 맛집의 전화번호를 전달받은 전화번호로 설정해라"
        // null이 들어올 수 있음 (전화번호를 입력하지 않은 경우)

        this.description = description;
        // "이 맛집의 소개글을 전달받은 소개글로 설정해라"
        // null이 들어올 수 있음 (소개글을 입력하지 않은 경우)

        this.member = member;
        // "이 맛집의 등록자를 전달받은 회원 객체로 설정해라"
        // DB에는 이 회원의 id(기본키)만 member_id 컬럼에 저장됨

        this.imageUrl = imageUrl;
        // [Phase 7] "이 맛집의 대표 이미지 URL을 설정해라"
        // null이 들어올 수 있음 (이미지를 업로드하지 않은 경우)

        this.representativeMenu = representativeMenu;
        // [추가]2026.11.11권성애 "이 맛집의 대표 메뉴를 설정해라" (null 가능)

        this.businessHours = businessHours;
        // [추가]2026.11.11권성애 "이 맛집의 영업시간을 설정해라" (null 가능)

        this.lat = lat;
        this.lng = lng;
        // [추가]2026.11.11권성애 "이 맛집의 위도 · 경도를 설정해라" (null 가능)

        this.source = (source != null) ? source : Source.MANUAL;
        // [추가]2026.11.11권성애 "출처를 설정해라. 지정하지 않으면 회원 직접 등록(MANUAL)"
        // RestaurantService.save()는 source를 넣지 않으므로 자동으로 MANUAL이 됩니다.

        this.createdAt = LocalDateTime.now();
        // 맛집 객체가 만들어지는 순간 = 맛집이 등록되는 순간이므로,
        // 등록일을 현재 날짜+시간으로 자동 설정합니다.
    }

    // ──────────────────────────────────────────────────────────
    // 메서드(Method) 영역
    // ──────────────────────────────────────────────────────────

    /**
     * 맛집 정보 수정 메서드
     *
     * [사용 위치]
     *   RestaurantService의 update() 메서드에서 호출됩니다.
     *   사용자가 맛집 수정 폼에서 내용을 변경하고 "저장" 버튼을 누르면,
     *   Controller → Service → 이 메서드 순으로 호출됩니다.
     *
     * [왜 setter(setName, setAddress 등) 대신 이 메서드를 쓰나?]
     *   setter를 public으로 열어놓으면 프로그램 어디서든 마음대로 값을 바꿀 수 있어 위험합니다.
     *   예: setName("") 처럼 이름을 빈 값으로 바꿔버릴 수 있음!
     *   update() 메서드 하나로 수정을 한 곳에서만 관리하면 더 안전합니다.
     *
     * [왜 id, member, createdAt은 매개변수에 없는가?]
     *   id: 기본키이므로 절대 변경하면 안 됨 (DB 무결성 파괴)
     *   member: 등록자는 바꿀 수 없음 (다른 사람의 맛집으로 둔갑 방지)
     *   createdAt: 등록일은 @Column(updatable=false)로 수정 불가
     *   [추가]2026.11.11권성애 source: 출처도 바꾸지 않음 (처음 들어온 경로를 유지)
     *
     * [JPA의 "변경 감지(Dirty Checking)" 기능]
     *   이 메서드로 필드 값을 바꾸기만 하면, JPA가 자동으로 UPDATE SQL을 실행합니다.
     *   즉, save()를 별도로 호출하지 않아도 트랜잭션이 끝나면 DB에 반영됩니다!
     *
     * @param name        수정할 가게 이름
     * @param category    수정할 카테고리
     * @param address     수정할 주소
     * @param phone       수정할 전화번호 (null 가능)
     * @param description 수정할 소개글 (null 가능)
     */
    public void update(String name, Category category, String address,
                       String phone, String description, String imageUrl) {
        this.name = name;
        // "이 맛집의 이름을 새로운 이름으로 변경해라"
        // 예: "김치찌개 명가" → "김치찌개 본가"

        this.category = category;
        // "이 맛집의 카테고리를 새로운 카테고리로 변경해라"
        // 예: Category.KOREAN → Category.ETC

        this.address = address;
        // "이 맛집의 주소를 새로운 주소로 변경해라"
        // 예: 가게가 이전한 경우

        this.phone = phone;
        // "이 맛집의 전화번호를 새로운 전화번호로 변경해라"
        // null이 들어올 수 있음 (전화번호를 삭제하는 경우)

        this.description = description;
        // "이 맛집의 소개글을 새로운 소개글로 변경해라"
        // null이 들어올 수 있음 (소개글을 삭제하는 경우)

        this.imageUrl = imageUrl;
        // [Phase 7] "이 맛집의 대표 이미지 URL을 새로운 URL로 변경해라"
        // null이 들어올 수 있음 (이미지를 삭제하는 경우)
    }

    /**
     * [추가]2026.11.11권성애 대표메뉴 · 영업시간 · 좌표 수정 메서드
     *
     * [왜 update()에 매개변수를 추가하지 않고 메서드를 나눴나?]
     *   update()의 매개변수를 바꾸면 그 메서드를 부르는 RestaurantService도 같이 고쳐야 합니다.
     *   등록 폼에 대표메뉴 · 영업시간 · 좌표 입력을 붙일 때 RestaurantService.update()에서
     *   update() 다음에 이 메서드를 같이 호출하면 됩니다.
     *
     * @param representativeMenu 수정할 대표 메뉴 (null 가능)
     * @param businessHours      수정할 영업시간 (null 가능)
     * @param lat                수정할 위도 (null 가능)
     * @param lng                수정할 경도 (null 가능)
     */
    public void updateDetail(String representativeMenu, String businessHours, Double lat, Double lng) {
        this.representativeMenu = representativeMenu;
        this.businessHours = businessHours;
        this.lat = lat;
        this.lng = lng;
    }

    // [추가]2026.11.11권성애 소프트 삭제: 행은 남기고 삭제 시각만 기록 (리뷰 · 댓글 외래키 유지)
    // 호출하는 곳: RestaurantService.delete()
    // @Transactional 안에서 값만 바꾸면 변경 감지로 UPDATE 가 실행됨 (save() 필요 없음)
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    // [추가]2026.11.11권성애 삭제된 맛집인지 (화면 · 서비스에서 확인용)
    public boolean isDeleted() {
        return deletedAt != null;
    }

    // ── [팀 프로젝트] AI 요약 저장 ───────────────────────────
    // 호출하는 곳: AiReviewSummaryService.summarize()
    // 이 메서드는 필드 값만 바꾼다. UPDATE SQL 은 save() 없이도 실행되는데,
    // @Transactional 이 끝날 때 Hibernate 의 영속성 컨텍스트(SessionImpl 객체 안의 PersistenceContext)가
    // 처음 읽어 온 값과 지금 값을 비교(변경 감지, dirty checking)해서 바뀐 컬럼만 UPDATE 하기 때문이다.
    public void updateAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
        this.aiSummaryAt = LocalDateTime.now();
    }

    // ── [팀 프로젝트] AI 요약 지우기 ─────────────────────────
    // 호출하는 곳: ReviewService.save(), ReviewService.delete()
    // 리뷰가 새로 생기거나 지워지면 예전 요약은 맞지 않으므로 비운다.
    // 다음에 "AI 요약 보기" 를 누르면 새 리뷰를 반영해 다시 만든다.
    public void clearAiSummary() {
        this.aiSummary = null;
        this.aiSummaryAt = null;
    }
}
