// ============================================================
// [RestaurantResponse.java] 맛집 조회 응답 데이터 전달 객체 (DTO)
// ============================================================
// 역할: 서버가 클라이언트(브라우저)에게 맛집 정보를 전달할 때 사용하는 "택배 상자"
//       Entity를 직접 보내지 않고, 필요한 정보만 골라서 이 DTO에 담아 전달합니다.
//
// ★ 왜 Entity를 직접 API 응답으로 보내면 안 되나요?
//   1) 보안 문제: Entity에는 비밀번호 같은 민감한 정보가 포함될 수 있음
//   2) 순환 참조: Entity 간의 양방향 관계가 무한 루프를 발생시킬 수 있음
//   3) 불필요한 정보: 클라이언트에 필요 없는 내부 필드까지 노출됨
//   4) 유연성: 화면마다 필요한 데이터가 다를 수 있음
//
// DTO vs Entity 차이:
//   Entity = DB 테이블과 직접 매핑되는 핵심 객체 (내부용)
//   Response DTO = 클라이언트에게 보여줄 데이터만 담은 객체 (외부 전달용)
//
// 사용 흐름 (서버 → 클라이언트):
//   1) Controller가 RestaurantService에 맛집 조회 요청
//   2) Service가 DB에서 Restaurant Entity를 조회
//   3) RestaurantResponse.from(entity, avgRating)으로 DTO 변환
//   4) Controller가 RestaurantResponse를 JSON으로 변환하여 응답
//   5) 프론트엔드(JavaScript)가 JSON을 받아 화면에 표시
//
// @Builder 패턴이란?
// ============================================================

// --- 패키지 선언 ---
// 이 파일이 속한 폴더(패키지) 경로를 지정합니다.
// com.example.mymatzip.dto = "DTO(데이터 전달 객체)" 계층에 해당
package com.example.mymatzip.dto;

// --- import 영역 (외부 클래스 가져오기) ---

// Category 열거형(enum) 가져오기 — 맛집의 음식 종류(한식, 중식, 일식 등)
// 응답에 카테고리 정보를 포함하기 위해 사용합니다.
import com.example.mymatzip.entity.Category;

// Restaurant 엔티티 가져오기 — from() 메서드에서 Entity → DTO 변환 시 사용
// Restaurant Entity의 getter를 호출하여 필요한 데이터를 DTO에 복사합니다.
import com.example.mymatzip.entity.Restaurant;

// @Builder 가져오기 — Lombok이 빌더 패턴 코드를 자동 생성
// .builder().필드명(값).필드명(값).build() 형태로 객체를 생성할 수 있게 해줍니다.
import lombok.Builder;

// @Getter 가져오기 — Lombok이 모든 필드의 getter 메서드를 자동 생성
// Spring이 객체를 JSON으로 변환할 때 getter를 사용하므로 응답 DTO에는 필수입니다.
// 예: getId(), getName(), getCategory() 등
import lombok.Getter;

// LocalDateTime 가져오기 — 날짜+시간을 표현하는 Java 표준 클래스
// 맛집이 등록된 날짜와 시간을 저장합니다. (예: 2024-03-15T14:30:00)
import java.time.LocalDateTime;

// @Getter 어노테이션 — 모든 필드에 대해 getter 메서드를 자동 생성
// Spring이 이 객체를 JSON으로 변환(직렬화)할 때 getter를 호출합니다.
// 예: { "id": 1, "name": "홍콩반점", ... } 형태의 JSON이 만들어짐
// ★ 응답 DTO에는 @Setter가 없습니다!
//   → 응답 객체는 한번 만들면 수정할 일이 없으므로 setter가 불필요합니다.
//   → @Builder로 생성할 때만 값을 설정하고, 이후에는 읽기만 합니다.
@Getter

// @Builder 어노테이션 — 빌더 패턴 코드를 자동 생성
// 이 어노테이션이 있으면 RestaurantResponse.builder()로 객체를 조립할 수 있습니다.
// 예: RestaurantResponse.builder()
//         .id(1L)
//         .name("홍콩반점")
//         .category(Category.CHINESE)
//         .build();
@Builder

// RestaurantResponse 클래스 선언
// 응답용 DTO = 서버 → 클라이언트로 맛집 데이터를 전달하기 위한 "택배 상자"
public class RestaurantResponse {

    // ──────────────────────────────────────────────────────
    // 맛집 고유 번호 (PK)
    // ──────────────────────────────────────────────────────
    // [타입] Long — 큰 정수 (예: 1L, 2L, 3L, ...)
    // [용도] 맛집을 고유하게 식별하는 번호 — 상세 페이지 URL, 수정/삭제 시 사용
    // [원본] Restaurant 엔티티의 id 필드에서 복사됨
    // ──────────────────────────────────────────────────────
    private Long id;                        // 맛집 고유 번호 (예: 1)

    // ──────────────────────────────────────────────────────
    // 맛집 이름
    // ──────────────────────────────────────────────────────
    // [타입] String — 문자열 (예: "홍콩반점", "스시오마카세")
    // [용도] 화면에 맛집 이름을 표시할 때 사용
    // [원본] Restaurant 엔티티의 name 필드에서 복사됨
    // ──────────────────────────────────────────────────────
    private String name;                    // 맛집 이름 (예: "홍콩반점")

    // ──────────────────────────────────────────────────────
    // 카테고리 (음식 종류 - enum 값)
    // ──────────────────────────────────────────────────────
    // [타입] Category — 열거형 (예: Category.KOREAN, Category.CHINESE)
    // [용도] 프론트엔드에서 카테고리별 필터링이나 분류에 사용
    // [원본] Restaurant 엔티티의 category 필드에서 복사됨
    // ──────────────────────────────────────────────────────
    private Category category;              // 음식 카테고리 enum (예: Category.CHINESE)

    // ──────────────────────────────────────────────────────
    // 카테고리 표시 이름 (한글)
    // ──────────────────────────────────────────────────────
    // [타입] String — 문자열 (예: "한식", "중식", "일식")
    // [용도] 화면에 사용자가 읽기 쉬운 한글 카테고리명을 표시할 때 사용
    //   → Category enum의 displayName 필드에서 가져옴
    //   → 예: Category.CHINESE → "중식", Category.KOREAN → "한식"
    // [원본] Restaurant 엔티티의 category.getDisplayName()에서 복사됨
    // ──────────────────────────────────────────────────────
    private String categoryDisplayName;     // 카테고리 한글 이름 (예: "중식")

    // ──────────────────────────────────────────────────────
    // 주소
    // ──────────────────────────────────────────────────────
    // [타입] String — 문자열 (예: "서울시 강남구 역삼동 123-45")
    // [용도] 화면에 맛집 주소를 표시하거나, 지도 API에 전달할 때 사용
    // [원본] Restaurant 엔티티의 address 필드에서 복사됨
    // ──────────────────────────────────────────────────────
    private String address;                 // 맛집 주소 (예: "서울시 강남구 역삼동 123-45")

    // ──────────────────────────────────────────────────────
    // 전화번호
    // ──────────────────────────────────────────────────────
    // [타입] String — 문자열 (예: "02-1234-5678") 또는 null
    // [용도] 화면에 맛집 전화번호를 표시할 때 사용
    // [원본] Restaurant 엔티티의 phone 필드에서 복사됨
    // [참고] 전화번호는 선택 입력이므로 null일 수 있습니다.
    // ──────────────────────────────────────────────────────
    private String phone;                   // 맛집 전화번호 (선택, 예: "02-1234-5678")

    // ──────────────────────────────────────────────────────
    // 설명
    // ──────────────────────────────────────────────────────
    // [타입] String — 문자열 (예: "점심 특선이 맛있는 중국집입니다.") 또는 null
    // [용도] 화면에 맛집 설명/소개를 표시할 때 사용
    // [원본] Restaurant 엔티티의 description 필드에서 복사됨
    // [참고] 설명은 선택 입력이므로 null일 수 있습니다.
    // ──────────────────────────────────────────────────────
    private String description;             // 맛집 설명 (선택, 예: "점심 특선이 맛있는 중국집")

    // ──────────────────────────────────────────────────────
    // 등록자 닉네임
    // ──────────────────────────────────────────────────────
    // [타입] String — 문자열 (예: "맛집탐험가")
    // [용도] 화면에 "등록자: 맛집탐험가" 형태로 표시할 때 사용
    // [원본] Restaurant 엔티티 → Member 엔티티의 nickname 필드에서 복사됨
    //   → restaurant.getMember().getNickname()으로 접근
    // ──────────────────────────────────────────────────────
    private String memberNickname;          // 맛집을 등록한 회원의 닉네임 (예: "맛집탐험가")

    // ──────────────────────────────────────────────────────
    // 평균 별점
    // ──────────────────────────────────────────────────────
    // [타입] Double — 실수 래퍼 클래스 (예: 4.3, 3.7, 0.0)
    //   ★ double(기본형) 대신 Double(래퍼 클래스)을 사용하는 이유:
    //     리뷰가 없으면 평균 별점이 null일 수 있으므로 null을 담을 수 있는 Double 사용
    // [용도] 화면에 "평균 ★ 4.3" 형태로 표시할 때 사용
    // [원본] ReviewRepository.averageRatingByRestaurantId()에서 계산된 값
    // [처리] from() 메서드에서 소수점 1자리로 반올림, null이면 0.0으로 변환
    // ──────────────────────────────────────────────────────
    private Double averageRating;           // 평균 별점 (예: 4.3, 리뷰 없으면 0.0)

    // ──────────────────────────────────────────────────────
    // 등록일시
    // ──────────────────────────────────────────────────────
    // [타입] LocalDateTime — 날짜+시간 (예: 2024-03-15T14:30:00)
    // [용도] 화면에 "등록일: 2024.03.15" 형태로 표시할 때 사용
    // [원본] Restaurant 엔티티의 createdAt 필드에서 복사됨
    //   → @CreatedDate 어노테이션으로 자동 설정된 값
    // ──────────────────────────────────────────────────────
    private LocalDateTime createdAt;        // 맛집 등록 일시 (예: 2024-03-15T14:30:00)

    // ──────────────────────────────────────────────────────
    // [Phase 7] 맛집 대표 이미지 URL
    // ──────────────────────────────────────────────────────
    // [타입] String — 문자열 (예: "/uploads/a1b2c3d4-...-e5f6.jpg") 또는 null
    // [용도] 화면에 맛집 대표 이미지를 표시할 때 사용
    //   → <img src="${imageUrl}"> 형태로 이미지를 화면에 렌더링
    // [원본] Restaurant 엔티티의 imageUrl 필드에서 복사됨
    // [참고] 이미지가 없으면 null → 기본 이미지(플레이스홀더)를 표시
    // ──────────────────────────────────────────────────────
    private String imageUrl;                // 맛집 대표 이미지 URL (선택, null 가능)

    // [추가]2026.11.11권성애 대표메뉴 · 영업시간 · 위도 · 경도 · 출처 · 기본 이미지
    private String representativeMenu;      // 대표메뉴 (선택)
    private String businessHours;           // 영업시간 (선택, 줄바꿈 포함 가능)
    private Double lat;                     // 위도
    private Double lng;                     // 경도
    private String sourceDisplayName;       // 출처 한글 이름 (예: "직접등록", "공공API 모범음식점") — 화면에 읽기 전용 표시
    private String defaultImageUrl;         // 카테고리 기본 이미지 (예: "/images/category/korean.svg") — 사진이 없거나 깨질 때

    // ──────────────────────────────────────────────────────
    // Entity → Response DTO 변환 메서드 (정적 팩토리 메서드)
    // ──────────────────────────────────────────────────────
    // [역할] Restaurant Entity를 RestaurantResponse DTO로 변환합니다.
    //
    // [매개변수]
    //   - Restaurant restaurant : DB에서 조회한 맛집 Entity 객체
    //     → 이 객체의 getter를 호출하여 필요한 데이터를 꺼냅니다.
    //   - Double averageRating  : ReviewRepository에서 계산한 평균 별점
    //     → null일 수 있음 (리뷰가 하나도 없는 경우)
    //
    // [반환값]
    //   - RestaurantResponse : 변환된 응답 DTO 객체
    //     → 이 객체가 JSON으로 변환되어 클라이언트에 전송됩니다.
    //
    // [static(정적) 메서드인 이유]
    //   RestaurantResponse 객체를 아직 만들기 전에 호출해야 하므로
    //   인스턴스 없이도 호출할 수 있는 static 메서드로 선언합니다.
    //   호출 방법: RestaurantResponse.from(restaurant, avgRating)
    //
    // [호출 예시 — RestaurantService에서]
    //   Restaurant restaurant = restaurantRepository.findById(id).orElseThrow(...);
    //   Double avg = reviewRepository.averageRatingByRestaurantId(id);
    //   RestaurantResponse response = RestaurantResponse.from(restaurant, avg);
    // ──────────────────────────────────────────────────────
    public static RestaurantResponse from(Restaurant restaurant, Double averageRating) {

        // @Builder 패턴으로 RestaurantResponse 객체를 하나씩 조립하여 생성합니다.
        return RestaurantResponse.builder()

                // restaurant.getId() → 맛집의 고유 번호를 id 필드에 설정
                .id(restaurant.getId())

                // restaurant.getName() → 맛집 이름을 name 필드에 설정
                .name(restaurant.getName())

                // restaurant.getCategory() → 카테고리 enum 값을 category 필드에 설정
                .category(restaurant.getCategory())

                // restaurant.getCategory().getDisplayName() → 카테고리의 한글 이름을 설정
                // 예: Category.CHINESE → "중식"
                .categoryDisplayName(restaurant.getCategory().getDisplayName())

                // restaurant.getAddress() → 주소를 address 필드에 설정
                .address(restaurant.getAddress())

                // restaurant.getPhone() → 전화번호를 phone 필드에 설정 (null일 수 있음)
                .phone(restaurant.getPhone())

                // restaurant.getDescription() → 설명을 description 필드에 설정 (null일 수 있음)
                .description(restaurant.getDescription())

                // restaurant.getMember().getNickname() → 등록자의 닉네임을 설정
                // Restaurant → Member 연관관계를 통해 닉네임에 접근
                .memberNickname(restaurant.getMember().getNickname())

                // 평균 별점 처리: null 체크 후 소수점 1자리로 반올림
                // averageRating != null ? → 평균 별점이 있으면(리뷰가 1개 이상이면)
                //   Math.round(averageRating * 10) / 10.0 → 소수점 1자리로 반올림
                //     예: 4.27 → 4.27 * 10 = 42.7 → Math.round(42.7) = 43 → 43 / 10.0 = 4.3
                //   : 0.0 → 리뷰가 없으면 0.0으로 설정
                .averageRating(averageRating != null ? Math.round(averageRating * 10) / 10.0 : 0.0)

                // restaurant.getCreatedAt() → 등록일시를 createdAt 필드에 설정
                .createdAt(restaurant.getCreatedAt())

                // [Phase 7] restaurant.getImageUrl() → 대표 이미지 URL을 설정 (null일 수 있음)
                .imageUrl(restaurant.getImageUrl())
                // [추가]2026.11.11권성애 새 항목
                .representativeMenu(restaurant.getRepresentativeMenu())
                .businessHours(restaurant.getBusinessHours())
                .lat(restaurant.getLat())
                .lng(restaurant.getLng())
                .sourceDisplayName(restaurant.getSource() != null ? restaurant.getSource().getDisplayName() : null)
                .defaultImageUrl(restaurant.getCategory().getDefaultImageUrl())

                // .build() → 위에서 설정한 모든 값으로 RestaurantResponse 객체를 최종 생성
                .build();
    }
}
