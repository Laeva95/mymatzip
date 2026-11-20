package com.example.mymatzip.entity;

// ============================================================
// [OwnerApplicationStatus.java] 식당 사장 권한 신청의 심사 상태
// ============================================================
//
// PENDING  → 신청 직후 상태. 관리자가 아직 처리하지 않음
// APPROVED → 관리자가 승인. 이 순간 신청자의 Role 이 USER → OWNER 로 바뀜
// REJECTED → 관리자가 반려. rejectReason 에 사유가 남음 (다시 신청 가능)
//
// DB 에는 name() 문자열("PENDING" 등)로 저장됩니다. (OwnerApplication.status 참고)
public enum OwnerApplicationStatus {

    PENDING("심사 중"),
    APPROVED("승인"),
    REJECTED("반려");

    private final String displayName; // 화면 표시용 한글 이름

    OwnerApplicationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
