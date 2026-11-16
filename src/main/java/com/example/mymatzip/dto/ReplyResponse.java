// ============================================================
// [ReplyResponse.java] 대댓글 조회 응답 데이터 전달 객체 (DTO)
// ============================================================
// 역할: 서버가 클라이언트(브라우저)에게 대댓글 정보를 전달할 때 사용하는 "택배 상자"
//       Reply Entity를 직접 보내지 않고, 필요한 정보만 골라서 이 DTO에 담아 전달합니다.
//
//
// 사용 흐름:
//   1) ReplyService에서 Reply 엔티티를 DB에서 조회
//   2) ReplyResponse.from(reply)로 DTO 변환
//   3) Controller가 JSON으로 변환하여 클라이언트에 전송
//
// 사용되는 곳:
//   - ReplyService (대댓글 조회, 작성 결과 반환)
//   - ReplyApiController (GET /api/replies, POST /api/replies)
// ============================================================

package com.example.mymatzip.dto;

// Reply 엔티티 가져오기 — from() 메서드에서 Entity → DTO 변환 시 사용
import com.example.mymatzip.entity.Reply;

// @Builder 가져오기 — Lombok이 빌더 패턴 코드를 자동 생성
import lombok.Builder;

// @Getter 가져오기 — Lombok이 모든 필드의 getter 메서드를 자동 생성
import lombok.Getter;

// LocalDateTime 가져오기 — 날짜+시간을 표현하는 Java 표준 클래스
import java.time.LocalDateTime;

@Getter
@Builder
public class ReplyResponse {

    // 대댓글 고유 번호 (삭제 시 사용)
    private Long id;

    // 대댓글 내용
    private String content;

    // 작성자 닉네임 (화면에 "작성자: 맛집탐험가" 표시용)
    private String memberNickname;

    // 작성자 회원 ID (프론트엔드에서 "내 답글인지" 판별용)
    private Long memberId;

    // 대댓글이 달린 리뷰의 ID
    private Long reviewId;

    // 작성 일시
    private LocalDateTime createdAt;

    // ──────────────────────────────────────────────────────
    // Entity → Response DTO 변환 정적 팩토리 메서드
    // ──────────────────────────────────────────────────────
    // [역할] Reply Entity를 ReplyResponse DTO로 변환합니다.
    //
    // [매개변수]
    //   - Reply reply : DB에서 조회한 대댓글 Entity 객체
    //
    // [반환값]
    //   - ReplyResponse : 변환된 응답 DTO 객체
    //
    // [호출 예시 — ReplyService에서]
    //   List<ReplyResponse> responses = replies.stream()
    //       .map(ReplyResponse::from).toList();
    // ──────────────────────────────────────────────────────
    public static ReplyResponse from(Reply reply) {
        return ReplyResponse.builder()
                .id(reply.getId())                                // 대댓글 고유 번호
                .content(reply.getContent())                      // 대댓글 내용
                .memberNickname(reply.getMember().getNickname())   // 작성자 닉네임
                .memberId(reply.getMember().getId())               // 작성자 회원 ID
                .reviewId(reply.getReview().getId())               // 대댓글이 달린 리뷰 ID
                .createdAt(reply.getCreatedAt())                   // 작성 일시
                .build();
    }
}