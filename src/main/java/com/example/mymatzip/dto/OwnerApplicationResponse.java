package com.example.mymatzip.dto;

import com.example.mymatzip.entity.OwnerApplication;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// ============================================================
// [OwnerApplicationResponse.java] 식당 사장 신청 응답 데이터
// ============================================================
// 사용처: 내 신청 조회(/api/owner-applications/me), 관리자 목록(/api/admin/owner-applications)
// 엔티티를 그대로 JSON 으로 내보내지 않고 필요한 값만 담습니다. (LAZY member 직렬화 오류 방지)
@Getter
@Builder
public class OwnerApplicationResponse {

    private Long id;
    private Long memberId;
    private String memberNickname;
    private String memberEmail;
    private String storeName;
    private String address;
    private String phone;
    private String businessNumber;
    private String message;
    private String status;            // "PENDING" / "APPROVED" / "REJECTED"
    private String statusDisplayName; // "심사 중" / "승인" / "반려"
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    public static OwnerApplicationResponse from(OwnerApplication a) {
        return OwnerApplicationResponse.builder()
                .id(a.getId())
                .memberId(a.getMember().getId())
                .memberNickname(a.getMember().getNickname())
                .memberEmail(a.getMember().getEmail())
                .storeName(a.getStoreName())
                .address(a.getAddress())
                .phone(a.getPhone())
                .businessNumber(a.getBusinessNumber())
                .message(a.getMessage())
                .status(a.getStatus().name())
                .statusDisplayName(a.getStatus().getDisplayName())
                .rejectReason(a.getRejectReason())
                .createdAt(a.getCreatedAt())
                .reviewedAt(a.getReviewedAt())
                .build();
    }
}
