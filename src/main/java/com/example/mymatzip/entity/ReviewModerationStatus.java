package com.example.mymatzip.entity;

/** 리뷰 AI 검토 상태 */
public enum ReviewModerationStatus {
    NORMAL,     // 정상 (공개)
    PENDING,    // 의심 키워드 있음, AI 배치 검토 대기 (공개)
    REVIEWING   // AI 배치 실행 중 (공개)
}
