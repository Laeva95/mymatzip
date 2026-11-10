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
//   @Column -- 컬럼 속성 설정 (nullable, updatable 등)
//   @ManyToOne -- 다대일 관계 설정 (여러 찜 → 한 명의 회원 / 한 곳의 맛집)
//   @JoinColumn -- 외래키(FK) 컬럼명 설정
//   FetchType -- 데이터 로딩 전략 (LAZY / EAGER)
//   GenerationType -- 기본키 생성 전략 (IDENTITY 등)

import lombok.AccessLevel;
// Lombok의 접근 수준 열거형. @NoArgsConstructor에서 PROTECTED 수준 지정에 사용
// PROTECTED = JPA 내부에서만 기본 생성자 사용 가능, 외부에서 빈 찜 객체 생성 방지

import lombok.Builder;
// Lombok의 빌더 패턴 자동 생성 어노테이션
// Wishlist.builder().member(member).restaurant(restaurant).build()
// 처럼 주문서 방식으로 찜 객체를 만들 수 있게 해줌

import lombok.Getter;
// Lombok의 getter 자동 생성 어노테이션
// 모든 필드에 대해 getXxx() 메서드를 자동으로 만들어줌
// 예: getId(), getMember(), getRestaurant(), getCreatedAt()

import lombok.NoArgsConstructor;
// Lombok의 기본 생성자 자동 생성 어노테이션
// JPA가 DB 데이터를 객체로 변환할 때 필요한 빈 생성자를 만들어줌

import java.time.LocalDateTime;
// 날짜+시간을 저장하는 자바 표준 클래스
// 찜한 날짜(createdAt) 필드에 사용
// 예: 2026-03-13T14:30:00

// ============================================================
// [Wishlist.java] 찜(위시리스트) 엔티티 -- DB의 wishlist 테이블과 1:1 매핑되는 클래스
// ============================================================
//
// 역할: "누가 어떤 맛집을 찜(저장)했는지" 기록하는 관계 테이블입니다.
//       이 클래스의 객체 하나 = DB wishlist 테이블의 행(row) 하나 = 찜 기록 1개
//
//
//
// DB 테이블 구조:
//   | id | member_id | restaurant_id | created_at          |
//   |----|-----------|---------------|---------------------|
//   | 1  | 2         | 1             | 2026-03-13 14:30:00 |
//   | 2  | 2         | 3             | 2026-03-13 15:00:00 |
//   | 3  | 3         | 1             | 2026-03-13 16:00:00 |
//   → "회원 2번(맛집탐험가)이 맛집 1번(김치찌개명가)을 찜함"
//   → "회원 2번(맛집탐험가)이 맛집 3번(스시오마카세)도 찜함"
//   → "회원 3번(먹방킹)이 맛집 1번(김치찌개명가)을 찜함"
//
// 관계(Relationship) -- "누가 + 어떤 맛집을" 연결하는 중간 테이블:
//   1. Member(회원) 1명 → Wishlist(찜) 여러 개 가능 (1:N 관계)
//
// 이 엔티티가 사용되는 곳들:
//   1. WishlistService -- 찜 추가/해제(토글), 찜 여부 확인 비즈니스 로직
//   2. WishlistRepository -- DB에서 찜 기록 조회 (findByMemberAndRestaurant 등)
//   3. WishlistApiController -- 찜 토글 API (찜 버튼 클릭 시 호출)
//   4. 타임리프 화면 -- 맛집 상세 페이지의 하트(찜) 아이콘 상태 표시
//     빨간 하트 = 이미 찜한 맛집 / 빈 하트 = 아직 찜하지 않은 맛집
// ============================================================

// @Entity: 이 클래스 = DB의 wishlist 테이블
//   JPA가 이 클래스를 보고 DB 테이블을 자동으로 만들거나 연결합니다.
//   테이블명: 클래스명(Wishlist) → 소문자 "wishlist"로 자동 변환
@Entity

// @Getter: 모든 필드의 getXxx() 메서드를 Lombok이 자동 생성
//   getId(), getMember(), getRestaurant(), getCreatedAt() -- 총 4개 자동 생성
@Getter

// @NoArgsConstructor(access = AccessLevel.PROTECTED)
//   JPA 전용 기본 생성자. protected로 제한하여 외부에서 빈 찜 객체 생성 방지
//   개발자는 아래의 @Builder를 통해서만 Wishlist 객체를 만들 수 있음
@NoArgsConstructor(access = AccessLevel.PROTECTED)

// public class Wishlist: 찜(위시리스트) 정보를 담는 공개 클래스
public class Wishlist {

    // ──────────────────────────────────────────────────────────
    // 필드(변수) 영역 -- DB 테이블의 각 컬럼(열)에 해당합니다
    // ──────────────────────────────────────────────────────────

    // @Id: 기본키(PK) 필드. 각 찜 기록을 유일하게 구분하는 번호
    @Id

    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    //   DB가 자동으로 번호를 매겨줌 (1, 2, 3, 4 ...)
    //   찜을 할 때마다 번호가 1씩 자동 증가
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    // Long id: 찜 기록의 고유 번호 (기본키)
    //   Long 타입 → null 허용 + 큰 수 저장 가능
    private Long id;

    // ──────────────────────────────────────────────────────────
    // 관계(Relationship) 필드 -- 다른 엔티티와의 연결을 표현합니다
    // Wishlist는 Member와 Restaurant 사이의 "중간 다리" 역할입니다.
    // "누가(Member) 어떤 맛집(Restaurant)을 찜했는지" 연결해주는 테이블
    // ──────────────────────────────────────────────────────────

    // @ManyToOne(fetch = FetchType.LAZY)
    //   읽는 법: 매니-투-원 (Many To One = 다대일)
    //
    //   뜻: "찜 기록 여러 개(Many)가 회원 1명(One)에게 속한다"
    //   다시 말해, 회원 1명이 맛집을 여러 개 찜할 수 있다는 관계입니다.
    //
    //   fetch = FetchType.LAZY (게으른 로딩):
    //     찜 기록을 가져올 때, 회원의 상세 정보(이메일, 닉네임 등)는 즉시 가져오지 않음
    //     getMember()를 호출하는 순간에야 DB에서 회원 정보를 조회합니다.
    @ManyToOne(fetch = FetchType.LAZY)

    // @JoinColumn(name = "member_id", nullable = false)
    //   DB의 wishlist 테이블에 "member_id"라는 컬럼을 만들어,
    //   찜한 회원의 번호(id)를 저장합니다.
    //
    //   예: 회원 id=2인 "맛집탐험가"가 찜 버튼을 누르면 → member_id = 2 저장
    //
    //   nullable = false → 주인 없는 찜 기록은 존재할 수 없음!
    //     반드시 "누가" 찜했는지 기록해야 합니다.
    //
    //   [사용 화면] 마이페이지 -- "내가 찜한 맛집" 목록 조회 시
    //     member_id = 로그인한 회원의 id 로 필터링하여 보여줌
    // [수정] 회원이 삭제되면 DB 가 이 행도 함께 지웁니다 (외래 키 ON DELETE CASCADE).
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "member_id", nullable = false)

    // Member member: 찜한 회원 객체
    //   Member 타입 → Member.java 엔티티의 객체
    //   이 필드를 통해 찜한 회원의 닉네임, 이메일 등에 접근 가능
    //   예: wishlist.getMember().getNickname() → "맛집탐험가"
    private Member member;

    // @ManyToOne(fetch = FetchType.LAZY)
    //   뜻: "찜 기록 여러 개(Many)가 맛집 1곳(One)에 속한다"
    //   다시 말해, 맛집 1곳이 여러 회원에게 찜될 수 있다는 관계입니다.
    //
    //   FetchType.LAZY: 찜 기록을 가져올 때 맛집 상세 정보는 나중에 필요할 때만 조회
    @ManyToOne(fetch = FetchType.LAZY)

    // @JoinColumn(name = "restaurant_id", nullable = false)
    //   DB의 wishlist 테이블에 "restaurant_id"라는 컬럼을 만들어,
    //   찜된 맛집의 번호(id)를 저장합니다.
    //
    //   예: 맛집 id=1인 "김치찌개명가"를 찜하면 → restaurant_id = 1 저장
    //
    //   nullable = false → 맛집 없는 찜 기록은 존재할 수 없음!
    //     반드시 "어떤 맛집을" 찜했는지 기록해야 합니다.
    //
    //   [사용 화면] 맛집 상세 페이지 -- 하트 아이콘 상태 결정
    //     member_id + restaurant_id 조합으로 "이 회원이 이 맛집을 찜했는지" 확인
    //     찜한 상태 → 빨간 하트(꽉 찬 하트) 표시
    //     찜 안 한 상태 → 빈 하트(테두리만 있는 하트) 표시
    // [수정] 맛집이 삭제되면 DB 가 이 행도 함께 지웁니다 (외래 키 ON DELETE CASCADE).
    //   이 줄이 없으면 리뷰·찜이 달린 맛집을 지울 때 외래 키 오류(500)가 납니다.
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "restaurant_id", nullable = false)

    // Restaurant restaurant: 찜된 맛집 객체
    //   Restaurant 타입 → Restaurant.java 엔티티의 객체
    //   이 필드를 통해 찜된 맛집의 이름, 주소 등에 접근 가능
    //   예: wishlist.getRestaurant().getName() → "김치찌개 명가"
    private Restaurant restaurant;

    // @Column(nullable = false, updatable = false)
    //   nullable = false → 찜한 날짜는 필수값 (비어있을 수 없음)
    //   updatable = false → 한 번 저장되면 수정 불가
    //     찜한 날짜는 수정할 이유가 없습니다.
    //     찜을 취소하고 다시 찜하면, 기존 기록을 수정하는 게 아니라
    //     기존 기록 삭제 + 새 기록 추가가 이루어집니다.
    //   [사용 화면] 찜 목록 정렬 -- 최근에 찜한 맛집 순서로 정렬할 때 사용
    @Column(nullable = false, updatable = false)

    // LocalDateTime createdAt: 찜한 날짜+시간
    //   예: 2026-03-13T14:30:00 (2026년 3월 13일 오후 2시 30분에 찜함)
    //   DB 컬럼명: created_at (camelCase → snake_case 자동 변환)
    private LocalDateTime createdAt;

    // ──────────────────────────────────────────────────────────
    // 생성자(Constructor) 영역
    // ──────────────────────────────────────────────────────────

    // @Builder: Lombok이 빌더 패턴 코드를 자동 생성
    //   사용 예시:
    //     Wishlist.builder()
    //         .member(loginMember)           // 찜하는 회원(로그인한 회원) 설정
    //         .restaurant(targetRestaurant)  // 찜할 맛집 설정
    //         .build();                      // 객체 생성 완료!
    //
    //   [참고] createdAt은 자동으로 현재 시간이 들어갑니다 (개발자가 넣을 필요 없음)
    //   [참고] id는 DB가 자동 생성하므로 넣지 않습니다.
    //   [참고] 매개변수가 member, restaurant 단 2개뿐으로, 가장 간단한 엔티티입니다.
    //
    //   실제 사용 흐름:
    //     1. 사용자가 맛집 상세 페이지에서 하트(찜) 버튼 클릭
    //     2. WishlistApiController가 요청을 받음
    //     3. WishlistService가 이미 찜했는지 확인
    //        → 찜 안 했으면: Wishlist.builder()로 새 찜 기록 생성 후 DB 저장
    //        → 이미 찜했으면: 기존 찜 기록을 DB에서 삭제 (찜 해제)
    //     4. 화면의 하트 아이콘 상태가 토글(빨간색 ↔ 빈 하트)됨
    @Builder

    // 매개변수 2개:
    //   Member member -- 찜하는 회원 객체 (필수)
    //   Restaurant restaurant -- 찜할 맛집 객체 (필수)
    public Wishlist(Member member, Restaurant restaurant) {
        this.member = member;
        // "이 찜 기록의 주인(찜한 회원)을 전달받은 회원 객체로 설정해라"
        // DB에는 이 회원의 id(기본키)만 member_id 컬럼에 저장됨

        this.restaurant = restaurant;
        // "이 찜 기록의 대상(찜된 맛집)을 전달받은 맛집 객체로 설정해라"
        // DB에는 이 맛집의 id(기본키)만 restaurant_id 컬럼에 저장됨

        this.createdAt = LocalDateTime.now();
        // LocalDateTime.now(): 현재 날짜+시간을 가져오는 메서드
        // 찜 객체가 만들어지는 순간 = 사용자가 찜 버튼을 누른 순간이므로,
        // 찜한 날짜를 현재 날짜+시간으로 자동 설정합니다.
    }
}