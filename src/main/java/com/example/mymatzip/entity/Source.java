package com.example.mymatzip.entity;

// ============================================================
// [Source.java] 맛집 데이터 출처 열거형 (Enum)
// ============================================================
// 역할: 맛집이 어디에서 들어왔는지 구분합니다.
//   - 회원이 등록 폼으로 직접 등록한 맛집인지
//   - 공공 API 배치로 수집한 맛집인지 (어느 API인지)
//
// 값을 정하는 곳 (폼에서 받지 않음 → 개발자 도구로 조작 불가):
//   - RestaurantService.save()           → MANUAL (Restaurant 생성자 기본값)
//   - PublicRestaurantImportService      → PUBLIC_API_BUSAN_FOOD / PUBLIC_API_BUSAN_MODEL
//
// DB 저장: Restaurant.source 컬럼에 상수 이름(영어)이 문자열로 저장됩니다.
//   화면에는 displayName(한글)을 보여 줍니다. (Category와 같은 방식)
// ============================================================

//[추가]2026.11.11권성애
public enum Source {

    PUBLIC_API_BUSAN_FOOD("공공API 부산맛집정보서비스"),   // 부산광역시_부산맛집정보 서비스 (FoodService/getFoodKr)
    PUBLIC_API_BUSAN_MODEL("공공API 모범음식점"),         // 부산광역시_구군 모범음식점 현황
    MANUAL("직접등록");                                   // 회원이 등록 폼으로 직접 등록

    private final String displayName;

    Source(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
