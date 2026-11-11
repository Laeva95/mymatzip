package com.example.mymatzip.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewForm {

    // 리뷰 내용 (필수)
    @NotBlank(message = "리뷰 내용을 입력해주세요.")
    @Size(max = 1000, message = "리뷰는 1000자 이내로 입력해주세요.")
    private String content;

    // 별점 (필수, 1~5점)
    @NotNull(message = "별점을 선택해주세요.")
    @Min(value = 1, message = "별점은 1점 이상이어야 합니다.")
    @Max(value = 5, message = "별점은 5점 이하여야 합니다.")
    private Integer rating;

    // 맛집 ID (필수)
    @NotNull(message = "맛집 ID가 필요합니다.")
    private Long restaurantId;

    // ──────────────────────────────────────────────────────
    // [Phase 7] 리뷰 이미지 URL 필드 (선택 입력)
    // ──────────────────────────────────────────────────────
    // ★ @Size와 @Pattern은 값이 null일 경우 검증을 통과시킵니다.
    //   따라서 선택 입력(optional)을 유지하면서도 잘못된 경로/길이 초과를 방어할 수 있습니다.
    // ──────────────────────────────────────────────────────
    @Size(max = 500, message = "이미지 경로가 너무 깁니다.")
    @Pattern(
            regexp = "^/uploads/[a-zA-Z0-9\\-]+\\.(jpg|jpeg|png|gif|webp)$",
            message = "올바른 이미지 경로 형식이 아닙니다."
    )
    private String imageUrl;
}