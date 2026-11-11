package com.example.mymatzip.entity; // 이 파일이 속한 패키지(폴더) 선언
// com.example.mymatzip.entity = entity 패키지 안에 있다는 뜻
// 같은 패키지 안의 Member.java, Restaurant.java를 import 없이 바로 사용 가능

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
//   @Column -- 컬럼 속성 설정 (nullable, length 등)
//   @ManyToOne -- 다대일 관계 설정 (여러 리뷰 → 한 명의 작성자 / 한 곳의 맛집)
//   @JoinColumn -- 외래키(FK) 컬럼명 설정
//   FetchType -- 데이터 로딩 전략 (LAZY / EAGER)
//   GenerationType -- 기본키 생성 전략 (IDENTITY 등)

import lombok.AccessLevel;
// Lombok의 접근 수준 열거형. @NoArgsConstructor에서 PROTECTED 수준 지정에 사용
// PROTECTED = JPA 내부에서만 기본 생성자 사용 가능, 외부에서 빈 리뷰 객체 생성 방지

import lombok.Builder;
// Lombok의 빌더 패턴 자동 생성 어노테이션
// Review.builder().content("맛있어요!").rating(5).member(m).restaurant(r).build()
// 처럼 주문서 방식으로 리뷰 객체를 만들 수 있게 해줌

import lombok.Getter;
// Lombok의 getter 자동 생성 어노테이션
// 모든 필드에 대해 getXxx() 메서드를 자동으로 만들어줌
// 예: getContent(), getRating(), getMember(), getRestaurant(), getCreatedAt()

import lombok.NoArgsConstructor;
// Lombok의 기본 생성자 자동 생성 어노테이션
// JPA가 DB 데이터를 객체로 변환할 때 필요한 빈 생성자를 만들어줌

import java.time.LocalDateTime;
// 날짜+시간을 저장하는 자바 표준 클래스
// 리뷰 작성일(createdAt) 필드에 사용
// 예: 2026-03-13T14:30:00

// ============================================================
// [Review.java] 리뷰 엔티티 -- DB의 review 테이블과 1:1 매핑되는 클래스
// ============================================================
//
// 역할: 맛집에 대한 리뷰(후기) 정보를 저장하는 DB 테이블을 자바 클래스로 표현한 것입니다.
//       이 클래스의 객체 하나 = DB review 테이블의 행(row) 하나 = 리뷰 1개
//
//
// DB 테이블 구조:
//   | id | content                  | rating | member_id | restaurant_id | created_at          |
//   |----|--------------------------|--------|-----------|---------------|---------------------|
//   | 1  | 김치찌개가 맛있어요!       | 5      | 2         | 1             | 2026-03-13 14:30:00 |
//   | 2  | 양이 조금 적어요          | 3      | 3         | 1             | 2026-03-13 15:00:00 |
//   → "회원 2번이 맛집 1번에 별점 5점 리뷰를 달았다"
//   → "회원 3번이 맛집 1번에 별점 3점 리뷰를 달았다"
//
// 관계(Relationship) -- 리뷰는 두 개의 관계를 가집니다:
//   1. Member(회원) 1명 → Review(리뷰) 여러 개 작성 가능 (1:N 관계)
//
// 이 엔티티가 사용되는 곳들:
//   1. ReviewService -- 리뷰 작성, 삭제 비즈니스 로직
//   2. ReviewRepository -- DB에서 리뷰 조회 (맛집별 리뷰 목록, 평균 별점 등)
//   3. ReviewApiController -- 리뷰 작성/삭제 API
//   4. ReviewResponse (DTO) -- 리뷰 정보를 화면에 전달할 때 사용
//   5. 타임리프 화면 -- 맛집 상세 페이지의 리뷰 목록, 별점 표시
// ============================================================

// @Entity: 이 클래스 = DB의 review 테이블
//   JPA가 이 클래스를 보고 DB 테이블을 자동으로 만들거나 연결합니다.
//   테이블명: 클래스명(Review) → 소문자 "review"로 자동 변환
@Entity

// @Getter: 모든 필드의 getXxx() 메서드를 Lombok이 자동 생성
//   getId(), getContent(), getRating(), getMember(), getRestaurant(), getCreatedAt()
//   총 6개의 getter 메서드가 자동으로 만들어집니다.
@Getter

// @NoArgsConstructor(access = AccessLevel.PROTECTED)
//   JPA 전용 기본 생성자. protected로 제한하여 외부에서 빈 리뷰 객체 생성 방지
//   개발자는 아래의 @Builder를 통해서만 Review 객체를 만들 수 있음
@NoArgsConstructor(access = AccessLevel.PROTECTED)

// public class Review: 리뷰 정보를 담는 공개 클래스
public class Review {

    // ──────────────────────────────────────────────────────────
    // 필드(변수) 영역 -- DB 테이블의 각 컬럼(열)에 해당합니다
    // ──────────────────────────────────────────────────────────

    // @Id: 기본키(PK) 필드. 리뷰를 유일하게 구분하는 번호
    @Id

    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    //   DB가 자동으로 번호를 매겨줌 (1, 2, 3, 4 ...)
    //   리뷰를 작성할 때마다 번호가 1씩 자동 증가
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    // Long id: 리뷰의 고유 번호 (기본키)
    //   Long 타입 → null 허용 + 큰 수 저장 가능
    //   아직 DB에 저장 안 된 새 리뷰는 id가 null
    private Long id;

    // @Column(nullable = false, length = 1000)
    //   nullable = false → 리뷰 내용은 필수! 빈 리뷰는 작성 불가
    @Column(nullable = false, length = 1000)

    // String content: 리뷰 내용(본문)을 저장하는 필드
    //   예: "김치찌개가 정말 맛있어요! 반찬도 푸짐하고 가격도 합리적이에요."
    //   예: "양이 조금 적어서 아쉬웠어요. 하지만 맛은 좋았습니다."
    private String content;

    // @Column(nullable = false): 별점도 필수값. 별점 없는 리뷰는 불가
    //   별점은 1~5 사이의 정수(integer) 값입니다.
    @Column(nullable = false)

    // int rating: 별점을 저장하는 필드 (1~5 사이의 정수)
    //   int 타입 → 정수(소수점 없는 숫자)를 저장. null 불가 (기본값 0)
    //   왜 Long이 아니라 int인가?
    //     별점은 1~5 사이의 작은 수이므로, 큰 수를 저장할 필요가 없음
    //     int는 약 -21억 ~ +21억까지 저장 가능 → 별점에 충분!
    //   왜 Integer가 아니라 int인가?
    //     int는 null 불가 → 별점에 null이 들어갈 일이 없으므로 int가 적합
    //   예: 5(최고), 4(좋음), 3(보통), 2(별로), 1(최악)
    private int rating;  // 1~5

    // ──────────────────────────────────────────────────────────
    // 관계(Relationship) 필드 -- 다른 엔티티와의 연결을 표현합니다
    // ──────────────────────────────────────────────────────────

    // @ManyToOne(fetch = FetchType.LAZY)
    //   읽는 법: 매니-투-원 (Many To One = 다대일)
    //
    //   뜻: "리뷰 여러 개(Many)가 회원 1명(One)에게 속한다"
    //   다시 말해, 회원 1명이 리뷰를 여러 개 작성할 수 있다는 관계입니다.
    //
    //   fetch = FetchType.LAZY (게으른 로딩):
    //     리뷰 정보를 DB에서 가져올 때, 작성자(Member) 정보는 즉시 가져오지 않음
    //     getMember()를 호출하는 순간에야 DB에서 회원 정보를 조회합니다.
    @ManyToOne(fetch = FetchType.LAZY)

    // @JoinColumn(name = "member_id", nullable = false)
    //   DB의 review 테이블에 "member_id"라는 컬럼을 만들어,
    //   리뷰 작성자의 회원 번호(id)를 저장합니다.
    //   예: 회원 id=2인 "맛집탐험가"가 리뷰를 작성하면 → member_id = 2 저장
    //
    //   nullable = false → 작성자 없는 리뷰는 존재할 수 없음! (익명 리뷰 불가)
    //
    //   [사용 화면] 리뷰 목록 -- "작성자: 맛집탐험가" 표시
    //   [사용 화면] 리뷰 삭제 -- 본인이 작성한 리뷰인지 확인 (member_id 비교)
    //   [사용 화면] 관리자(ADMIN)는 다른 사람의 리뷰도 삭제 가능
    // [수정] 회원이 삭제되면 DB 가 이 행도 함께 지웁니다 (외래 키 ON DELETE CASCADE).
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "member_id", nullable = false)

    // Member member: 이 리뷰를 작성한 회원 객체
    //   Member 타입 → Member.java 엔티티의 객체
    //   이 필드를 통해 작성자의 닉네임, 이메일 등에 접근 가능
    //   예: review.getMember().getNickname() → "맛집탐험가"
    private Member member;

    // @ManyToOne(fetch = FetchType.LAZY)
    //   뜻: "리뷰 여러 개(Many)가 맛집 1곳(One)에 속한다"
    //   다시 말해, 맛집 1곳에 여러 개의 리뷰가 달릴 수 있다는 관계입니다.
    //
    //   FetchType.LAZY: 리뷰를 가져올 때 맛집 상세 정보는 나중에 필요할 때만 조회
    @ManyToOne(fetch = FetchType.LAZY)

    // @JoinColumn(name = "restaurant_id", nullable = false)
    //   DB의 review 테이블에 "restaurant_id"라는 컬럼을 만들어,
    //   리뷰 대상 맛집의 번호(id)를 저장합니다.
    //   예: 맛집 id=1인 "김치찌개명가"에 리뷰를 작성하면 → restaurant_id = 1 저장
    //
    //   nullable = false → 맛집 없는 리뷰는 존재할 수 없음! (반드시 어떤 맛집에 대한 리뷰)
    //
    //   [사용 화면] 맛집 상세 페이지 -- restaurant_id로 해당 맛집의 리뷰만 필터링하여 표시
    //   [참고] 맛집이 삭제되면 이 맛집에 달린 리뷰들도 함께 처리해야 합니다.
    // [수정] 맛집이 삭제되면 DB 가 이 행도 함께 지웁니다 (외래 키 ON DELETE CASCADE).
    //   이 줄이 없으면 리뷰·찜이 달린 맛집을 지울 때 외래 키 오류(500)가 납니다.
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "restaurant_id", nullable = false)

    // Restaurant restaurant: 이 리뷰가 달린 맛집 객체
    //   Restaurant 타입 → Restaurant.java 엔티티의 객체
    //   이 필드를 통해 맛집의 이름, 주소 등에 접근 가능
    //   예: review.getRestaurant().getName() → "김치찌개 명가"
    private Restaurant restaurant;

    // @Column(nullable = false, updatable = false)
    //   nullable = false → 작성일은 필수값 (비어있을 수 없음)
    //   updatable = false → 한 번 저장되면 수정 불가
    //     리뷰 작성일은 수정할 이유가 없으므로 보호합니다.
    @Column(nullable = false, updatable = false)

    // LocalDateTime createdAt: 리뷰 작성 날짜+시간
    //   예: 2026-03-13T14:30:00 (2026년 3월 13일 오후 2시 30분)
    //   DB 컬럼명: created_at (camelCase → snake_case 자동 변환)
    private LocalDateTime createdAt;

    // [Phase 7] 리뷰 이미지 URL (선택값, null 가능)
    // 리뷰에 사진을 첨부하면 이 필드에 파일 경로가 저장됩니다.
    // 예: "/uploads/a1b2c3d4-...-e5f6.jpg"
    // 예: null (이미지를 첨부하지 않은 경우)
    @Column(length = 500) // 또는 1000
    private String imageUrl;


    // [Phase 10] 신고 여부 (기본값: false)
    // 다른 사용자가 이 리뷰를 신고하면 true로 변경됩니다.
    @Column(nullable = false)
    private boolean reported = false;

    // ──────────────────────────────────────────────────────────
    // 생성자(Constructor) 영역
    // ──────────────────────────────────────────────────────────

    // @Builder: Lombok이 빌더 패턴 코드를 자동 생성
    //   사용 예시:
    //     Review.builder()
    //         .content("김치찌개가 정말 맛있어요!")  // 리뷰 내용 설정
    //         .rating(5)                           // 별점 5점 설정
    //         .member(loginMember)                 // 작성자(로그인한 회원) 설정
    //         .restaurant(targetRestaurant)        // 리뷰 대상 맛집 설정
    //         .build();                            // 객체 생성 완료!
    //
    //   [참고] createdAt은 자동으로 현재 시간이 들어갑니다.
    //   [참고] id는 DB가 자동 생성하므로 넣지 않습니다.
    //   [참고] 모든 매개변수(content, rating, member, restaurant)는 필수입니다.
    @Builder

    // 매개변수 4개:
    //   String content -- 리뷰 내용/본문 (필수, 최대 1000자)
    //   int rating -- 별점 (필수, 1~5 사이 정수)
    //   Member member -- 리뷰 작성자 회원 객체 (필수)
    //   Restaurant restaurant -- 리뷰 대상 맛집 객체 (필수)
    // 매개변수 5개:
    //   String content -- 리뷰 내용/본문 (필수, 최대 1000자)
    //   int rating -- 별점 (필수, 1~5 사이 정수)
    //   Member member -- 리뷰 작성자 회원 객체 (필수)
    //   Restaurant restaurant -- 리뷰 대상 맛집 객체 (필수)
    //   String imageUrl -- [Phase 7] 리뷰 이미지 URL (선택, null 가능)
    public Review(String content, int rating, Member member, Restaurant restaurant,
                  String imageUrl) {
        this.content = content;
        // "이 리뷰의 내용을 전달받은 내용으로 설정해라"
        // 예: "김치찌개가 정말 맛있어요! 반찬도 푸짐합니다."

        this.rating = rating;
        // "이 리뷰의 별점을 전달받은 별점으로 설정해라"
        // 예: 5 (최고!), 3 (보통), 1 (별로...)
        // int 타입이므로 기본값은 0이지만, 실제로는 1~5 사이의 값이 들어옴

        this.member = member;
        // "이 리뷰의 작성자를 전달받은 회원 객체로 설정해라"
        // DB에는 이 회원의 id(기본키)만 member_id 컬럼에 저장됨
        // 예: 로그인한 회원 객체가 여기에 들어감

        this.restaurant = restaurant;
        // "이 리뷰의 대상 맛집을 전달받은 맛집 객체로 설정해라"
        // DB에는 이 맛집의 id(기본키)만 restaurant_id 컬럼에 저장됨
        // 예: 리뷰를 작성하려는 맛집 상세 페이지의 맛집 객체가 여기에 들어감

        this.imageUrl = imageUrl;
        // [Phase 7] "이 리뷰의 이미지 URL을 설정해라"
        // null이 들어올 수 있음 (이미지를 첨부하지 않은 경우)

        this.createdAt = LocalDateTime.now();
        // LocalDateTime.now(): 현재 날짜+시간을 가져오는 메서드
        // 리뷰 객체가 만들어지는 순간 = 리뷰가 작성되는 순간이므로,
        // 작성일을 현재 날짜+시간으로 자동 설정합니다.
    }

    // ──────────────────────────────────────────────────────────
    // [Phase 10] 리뷰 신고 메서드
    // ──────────────────────────────────────────────────────────
    // 다른 사용자가 부적절한 리뷰를 신고할 때 호출됩니다.
    // reported 필드를 true로 변경하여 관리자가 검토할 수 있게 합니다.
    //
    // [JPA 변경 감지] 이 메서드로 값을 바꾸면 트랜잭션 종료 시 자동으로 UPDATE됨
    public void report() {
        this.reported = true;
    }
}