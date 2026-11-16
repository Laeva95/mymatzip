// ============================================================
// [ReplyRepository.java] 대댓글 데이터 접근 인터페이스 (Repository)
// ============================================================
// 역할: 대댓글(Reply) 테이블에 대한 DB 접근(CRUD)을 담당합니다.
//       JpaRepository를 상속받으면 기본 CRUD 메서드가 자동으로 제공됩니다.
//
//
// ★ 핵심: @EntityGraph로 N+1 문제를 방지하고,
//   메서드 이름 규칙으로 자동으로 SQL이 생성됩니다.
//
// 사용되는 곳: ReplyService (대댓글 작성, 조회, 삭제)
// ============================================================

package com.example.mymatzip.repository;

// Reply 엔티티 가져오기 — 이 Repository가 다룰 대상(테이블)
import com.example.mymatzip.entity.Reply;

// JpaRepository 가져오기 — Spring Data JPA가 제공하는 "만능 창구" 인터페이스
import org.springframework.data.jpa.repository.JpaRepository;

// @EntityGraph 가져오기 — N+1 문제를 해결하는 어노테이션
// 대댓글 목록을 가져올 때 작성자 정보도 한꺼번에 가져오기 위해 사용
import org.springframework.data.jpa.repository.EntityGraph;

// List 가져오기 — 여러 개의 대댓글을 담는 "목록" 자료구조
import java.util.List;

// ★ interface 선언 — Spring Data JPA가 자동으로 구현체를 생성
// extends JpaRepository<Reply, Long>: Reply 엔티티를 다루며, PK 타입은 Long
public interface ReplyRepository extends JpaRepository<Reply, Long> {

    // ──────────────────────────────────────────────────────
    // 특정 리뷰의 대댓글 목록 조회 메서드 (오래된 순 정렬)
    // ──────────────────────────────────────────────────────
    // [사용 화면] 맛집 상세 페이지 → 리뷰 아래 대댓글 목록 영역
    //
    // [매개변수]
    //   - Long reviewId : 대댓글을 조회할 리뷰의 고유 번호(ID)
    //
    // [반환값]
    //   - List<Reply> : 해당 리뷰에 달린 대댓글 목록 (오래된 순)
    //     → 대댓글은 시간순(Asc, 오래된 것부터)으로 정렬합니다.
    //     → 이유: 대화의 흐름을 자연스럽게 보여주기 위해 (오래된 답글부터 순서대로)
    //
    // [Query Method 해석]
    //   find + By + ReviewId + OrderBy + CreatedAt + Asc
    //   → SELECT * FROM reply WHERE review_id = ? ORDER BY created_at ASC
    //
    // @EntityGraph: 대댓글 작성자(member) 정보를 JOIN으로 한꺼번에 가져옴
    // → N+1 문제 방지: 대댓글 10개 조회 시 작성자 조회 쿼리 10번 → 1번으로 줄임
    // ──────────────────────────────────────────────────────
    @EntityGraph(attributePaths = {"member"})
    List<Reply> findByReviewIdOrderByCreatedAtAsc(Long reviewId);

    // ──────────────────────────────────────────────────────
    // 특정 회원이 작성한 대댓글 목록 조회 메서드
    // ──────────────────────────────────────────────────────
    // [사용 화면] 마이페이지 → "내가 쓴 대댓글" 목록
    //
    // [매개변수]
    //   - Long memberId : 조회할 회원의 고유 번호(ID)
    //
    // [반환값]
    //   - List<Reply> : 해당 회원이 작성한 대댓글 목록
    //
    // [Query Method 해석]
    //   find + By + MemberId
    //   → SELECT * FROM reply WHERE member_id = ?
    // ──────────────────────────────────────────────────────
    List<Reply> findByMemberId(Long memberId);
}