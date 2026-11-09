// ============================================================
// [Reply.java] 대댓글 엔티티 — DB의 reply 테이블과 1:1 매핑되는 클래스
// ============================================================
// 역할: 리뷰에 달리는 대댓글(답글) 정보를 저장하는 DB 테이블을
//       자바 클래스로 표현한 것입니다.
//       이 클래스의 객체 하나 = DB reply 테이블의 행(row) 하나 = 대댓글 1개
//
//
// DB 테이블 구조:
//   | id | content            | review_id | member_id | created_at          |
//   |----|--------------------|-----------|-----------|---------------------|
//   | 1  | 저도 같은 생각이에요! | 1         | 3         | 2026-03-14 10:00:00 |
//   | 2  | 감사합니다~         | 1         | 2         | 2026-03-14 10:30:00 |
//   → "회원 3번이 리뷰 1번에 대댓글을 달았다"
//
// 관계(Relationship):
//   1. Review(리뷰) 1개 → Reply(대댓글) 여러 개 달릴 수 있음 (1:N 관계)
//
// 사용되는 곳:
//   1. ReplyService — 대댓글 작성, 삭제 비즈니스 로직
//   2. ReplyRepository — DB에서 대댓글 조회
//   3. ReplyApiController — 대댓글 CRUD API
//   4. ReplyResponse (DTO) — 대댓글 정보를 화면에 전달
// ============================================================

package com.example.mymatzip.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
// JPA 관련 어노테이션:
//   @Entity, @Id, @GeneratedValue, @Column, @ManyToOne, @JoinColumn
//   FetchType, GenerationType

import lombok.AccessLevel;
// 접근 수준 열거형. @NoArgsConstructor에서 PROTECTED 수준 지정에 사용

import lombok.Builder;
// 빌더 패턴 자동 생성 어노테이션

import lombok.Getter;
// 모든 필드의 getter 메서드를 자동 생성

import lombok.NoArgsConstructor;
// JPA 전용 기본 생성자 자동 생성

import java.time.LocalDateTime;
// 날짜+시간 저장용 클래스

// @Entity: 이 클래스 = DB의 reply 테이블
@Entity

// @Getter: 모든 필드의 getXxx() 메서드를 자동 생성
@Getter

// @NoArgsConstructor(access = AccessLevel.PROTECTED)
//   JPA 전용 기본 생성자. 외부에서 빈 대댓글 객체 생성 방지
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reply {

    // ── 기본키(PK) 필드 ──
    // 대댓글의 고유 번호. DB가 자동으로 번호를 매겨줍니다 (1, 2, 3, ...)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── 대댓글 내용 ──
    // @Column(nullable = false, length = 500)
    //   nullable = false → 내용 없는 대댓글은 불가
    //   length = 500 → 최대 500자까지 작성 가능
    @Column(nullable = false, length = 500)
    private String content;

    // ── 리뷰 연관관계 (대댓글이 달린 리뷰) ──
    // @ManyToOne: "대댓글 여러 개(Many)가 리뷰 1개(One)에 속한다"
    // FetchType.LAZY: 대댓글을 가져올 때 리뷰 정보는 나중에 필요할 때만 조회
    @ManyToOne(fetch = FetchType.LAZY)
    // [수정] 리뷰가 삭제되면(맛집 삭제로 함께 지워질 때 포함) DB 가 이 답글도 함께 지웁니다.
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    // ── 작성자 연관관계 ──
    // @ManyToOne: "대댓글 여러 개(Many)가 회원 1명(One)에게 속한다"
    // FetchType.LAZY: 대댓글을 가져올 때 작성자 정보는 나중에 필요할 때만 조회
    @ManyToOne(fetch = FetchType.LAZY)
    // [수정] 회원이 삭제되면 DB 가 이 행도 함께 지웁니다 (외래 키 ON DELETE CASCADE).
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // ── 작성 일시 ──
    // 대댓글이 작성된 날짜+시간. 한 번 저장되면 수정 불가
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── 생성자 (Builder 패턴) ──
    // @Builder: Reply.builder().content("좋은 리뷰입니다!").review(review).member(member).build()
    // createdAt은 자동으로 현재 시간이 설정됩니다.
    @Builder
    public Reply(String content, Review review, Member member) {
        this.content = content;
        this.review = review;
        this.member = member;
        this.createdAt = LocalDateTime.now();
    }
}