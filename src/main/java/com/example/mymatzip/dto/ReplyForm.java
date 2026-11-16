// ============================================================
// [ReplyForm.java] 대댓글 작성 요청 데이터 전달 객체 (DTO)
// ============================================================
// 역할: 사용자가 대댓글 작성 시 입력한 내용과 리뷰 ID를 담는 "택배 상자"
//
//
// 사용 흐름:
//   1) 사용자가 리뷰 아래 대댓글 입력란에 내용을 입력
//   2) JavaScript의 fetch()가 JSON으로 서버에 전송
//      예: { "reviewId": 1, "content": "저도 같은 생각이에요!" }
//   3) Spring이 JSON을 ReplyForm 객체로 자동 변환 (@RequestBody)
//   4) @Valid로 검증 → 통과하면 ReplyService.save()에 전달
//
// 사용되는 곳: ReplyApiController (POST /api/replies)
// ============================================================

package com.example.mymatzip.dto;

// 검증 어노테이션 가져오기
// @NotNull: null 값 거부 (Long 같은 비문자열 타입에 사용)
// @NotBlank: 빈 문자열 거부 (String 전용)
// @Size: 문자열 길이 제한
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// @Getter, @Setter 가져오기 — Lombok이 getter/setter 메서드를 자동 생성
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReplyForm {

    // ──────────────────────────────────────────────────────
    // 리뷰 ID 필드
    // ──────────────────────────────────────────────────────
    // 대댓글이 달릴 대상 리뷰의 고유 번호(ID)
    // 프론트엔드에서 자동으로 전송되므로 사용자가 직접 입력하지 않음
    //
    // @NotNull: 리뷰 ID가 없으면 어떤 리뷰에 답글을 달지 알 수 없으므로 필수!
    // ──────────────────────────────────────────────────────
    @NotNull(message = "리뷰 ID가 필요합니다.")
    private Long reviewId;

    // ──────────────────────────────────────────────────────
    // 대댓글 내용 필드
    // ──────────────────────────────────────────────────────
    // 사용자가 입력한 대댓글 본문 텍스트
    //
    // @NotBlank: 빈 답글은 작성 불가 (null, "", "  " 모두 거부)
    // @Size(max=500): 대댓글은 최대 500자까지 작성 가능
    //   → Reply 엔티티의 content 컬럼 길이(500)에 맞춤
    // ──────────────────────────────────────────────────────
    @NotBlank(message = "대댓글 내용을 입력해주세요.")
    @Size(max = 500, message = "대댓글은 500자 이내로 입력해주세요.")
    private String content;
}