// ============================================================
// [ReviewResponse.java] 리뷰 조회 응답 데이터 전달 객체 (DTO)
// ============================================================
// 역할: 서버가 클라이언트(브라우저)에게 리뷰 정보를 전달할 때 사용하는 "택배 상자"
//       Review Entity를 직접 보내지 않고, 필요한 정보만 골라서 이 DTO에 담아 전달합니다.
//
// ★ 왜 Entity를 직접 API 응답으로 보내면 안 되나요?
//   1) 보안 문제: Entity에는 민감한 정보가 포함될 수 있음
//   2) 순환 참조: Review → Restaurant → Review 같은 양방향 관계가 무한 루프 발생
//   3) 불필요한 정보: 클라이언트에 필요 없는 내부 필드까지 노출됨
//
// DTO vs Entity 차이:
//   Entity = DB 테이블과 직접 매핑되는 핵심 객체 (내부용)
//   Response DTO = 클라이언트에게 보여줄 데이터만 담은 객체 (외부 전달용)
//
// 사용 흐름 (서버 → 클라이언트):
//   1) Controller가 ReviewService에 리뷰 목록 조회 요청
//   2) Service가 DB에서 Review Entity 목록을 조회
//   3) 각 Review Entity를 ReviewResponse.from(review)으로 DTO 변환
//   4) Controller가 ReviewResponse 목록을 JSON으로 변환하여 응답
//   5) 프론트엔드(JavaScript)가 JSON을 받아 리뷰 목록을 화면에 표시
//
// @Builder 패턴이란?
// ============================================================

// --- 패키지 선언 ---
// 이 파일이 속한 폴더(패키지) 경로를 지정합니다.
// com.example.mymatzip.dto = "DTO(데이터 전달 객체)" 계층에 해당
package com.example.mymatzip.dto;

// --- import 영역 (외부 클래스 가져오기) ---

// Review 엔티티 가져오기 — from() 메서드에서 Entity → DTO 변환 시 사용
// Review Entity의 getter를 호출하여 필요한 데이터를 DTO에 복사합니다.
import com.example.mymatzip.entity.Review;

// @Builder 가져오기 — Lombok이 빌더 패턴 코드를 자동 생성
// .builder().필드명(값).필드명(값).build() 형태로 객체를 생성할 수 있게 해줍니다.
import lombok.Builder;

// @Getter 가져오기 — Lombok이 모든 필드의 getter 메서드를 자동 생성
// Spring이 객체를 JSON으로 변환할 때 getter를 사용하므로 응답 DTO에는 필수입니다.
import lombok.Getter;

// LocalDateTime 가져오기 — 날짜+시간을 표현하는 Java 표준 클래스
// 리뷰가 작성된 날짜와 시간을 저장합니다. (예: 2024-03-15T14:30:00)
import java.time.LocalDateTime;

// @Getter 어노테이션 — 모든 필드에 대해 getter 메서드를 자동 생성
// Spring이 이 객체를 JSON으로 변환(직렬화)할 때 getter를 호출합니다.
// 예: { "id": 1, "content": "맛있어요!", "rating": 5, ... } 형태의 JSON이 만들어짐
// ★ 응답 DTO에는 @Setter가 없습니다!
//   → 응답 객체는 한번 만들면 수정할 일이 없으므로 setter가 불필요합니다.
@Getter

// @Builder 어노테이션 — 빌더 패턴 코드를 자동 생성
// 이 어노테이션이 있으면 ReviewResponse.builder()로 객체를 조립할 수 있습니다.
// 예: ReviewResponse.builder()
//         .id(1L)
//         .content("맛있어요!")
//         .rating(5)
//         .build();
@Builder

// ReviewResponse 클래스 선언
// 응답용 DTO = 서버 → 클라이언트로 리뷰 데이터를 전달하기 위한 "택배 상자"
public class ReviewResponse {

    // ──────────────────────────────────────────────────────
    // 리뷰 고유 번호 (PK)
    // ──────────────────────────────────────────────────────
    // [타입] Long — 큰 정수 (예: 1L, 2L, 3L, ...)
    // [용도] 리뷰를 고유하게 식별하는 번호 — 수정/삭제 시 사용
    // [원본] Review 엔티티의 id 필드에서 복사됨
    // ──────────────────────────────────────────────────────
    private Long id;                        // 리뷰 고유 번호 (예: 1)

    // ──────────────────────────────────────────────────────
    // 리뷰 내용
    // ──────────────────────────────────────────────────────
    // [타입] String — 문자열 (예: "여기 돈까스 정말 맛있어요!")
    // [용도] 화면에 리뷰 본문 텍스트를 표시할 때 사용
    // [원본] Review 엔티티의 content 필드에서 복사됨
    // ──────────────────────────────────────────────────────
    private String content;                 // 리뷰 내용 (예: "여기 돈까스 정말 맛있어요!")

    // ──────────────────────────────────────────────────────
    // 별점
    // ──────────────────────────────────────────────────────
    // [타입] int — 기본형 정수 (1 ~ 5 사이의 값)
    //   ★ Integer 대신 int(기본형)을 사용하는 이유:
    //     응답 DTO에서는 이미 DB에서 조회한 확정 값이므로 null일 가능성이 없습니다.
    //     따라서 메모리를 절약할 수 있는 기본형(int)을 사용합니다.
    // [용도] 화면에 ★★★★☆ 형태로 별점을 표시할 때 사용
    // [원본] Review 엔티티의 rating 필드에서 복사됨
    // ──────────────────────────────────────────────────────
    private int rating;                     // 별점 (1~5, 예: 4)

    // ──────────────────────────────────────────────────────
    // 작성자 닉네임
    // ──────────────────────────────────────────────────────
    // [타입] String — 문자열 (예: "맛집탐험가")
    // [용도] 화면에 "작성자: 맛집탐험가" 형태로 표시할 때 사용
    // [원본] Review 엔티티 → Member 엔티티의 nickname 필드에서 복사됨
    //   → review.getMember().getNickname()으로 접근
    // ──────────────────────────────────────────────────────
    private String memberNickname;          // 리뷰 작성자의 닉네임 (예: "맛집탐험가")

    // ──────────────────────────────────────────────────────
    // 작성자 회원 ID
    // ──────────────────────────────────────────────────────
    // [타입] Long — 큰 정수 (예: 3L)
    // [용도] 프론트엔드에서 "이 리뷰가 현재 로그인한 사용자가 쓴 것인지" 판별할 때 사용
    //   → 로그인한 사용자의 ID와 비교하여 수정/삭제 버튼 표시 여부를 결정
    // [원본] Review 엔티티 → Member 엔티티의 id 필드에서 복사됨
    //   → review.getMember().getId()로 접근
    // ──────────────────────────────────────────────────────
    private Long memberId;                  // 리뷰 작성자의 회원 ID (예: 3)

    // ──────────────────────────────────────────────────────
    // 맛집 ID
    // ──────────────────────────────────────────────────────
    // [타입] Long — 큰 정수 (예: 5L)
    // [용도] 이 리뷰가 어떤 맛집에 대한 리뷰인지 식별할 때 사용
    //   → 프론트엔드에서 맛집 상세 페이지 링크 등에 활용
    // [원본] Review 엔티티 → Restaurant 엔티티의 id 필드에서 복사됨
    //   → review.getRestaurant().getId()로 접근
    // ──────────────────────────────────────────────────────
    private Long restaurantId;              // 리뷰 대상 맛집의 ID (예: 5)

    // ──────────────────────────────────────────────────────
    // 작성일시
    // ──────────────────────────────────────────────────────
    // [타입] LocalDateTime — 날짜+시간 (예: 2024-03-15T14:30:00)
    // [용도] 화면에 "작성일: 2024.03.15" 형태로 표시할 때 사용
    // [원본] Review 엔티티의 createdAt 필드에서 복사됨
    //   → @CreatedDate 어노테이션으로 자동 설정된 값
    // ──────────────────────────────────────────────────────
    private LocalDateTime createdAt;        // 리뷰 작성 일시 (예: 2024-03-15T14:30:00)

    // ──────────────────────────────────────────────────────
    // [Phase 7] 리뷰 이미지 URL
    // ──────────────────────────────────────────────────────
    // [타입] String — 문자열 (예: "/uploads/a1b2c3d4-...-e5f6.jpg") 또는 null
    // [용도] 화면에 리뷰 첨부 이미지를 표시할 때 사용
    // [원본] Review 엔티티의 imageUrl 필드에서 복사됨
    // ──────────────────────────────────────────────────────
    private String imageUrl;                // 리뷰 이미지 URL (선택, null 가능)

    // ──────────────────────────────────────────────────────
    // Entity → Response DTO 변환 메서드 (정적 팩토리 메서드)
    // ──────────────────────────────────────────────────────
    // [역할] Review Entity를 ReviewResponse DTO로 변환합니다.
    //
    // [매개변수]
    //   - Review review : DB에서 조회한 리뷰 Entity 객체
    //     → 이 객체의 getter를 호출하여 필요한 데이터를 꺼냅니다.
    //     → review.getMember()로 작성자 정보에 접근
    //     → review.getRestaurant()로 맛집 정보에 접근
    //
    // [반환값]
    //   - ReviewResponse : 변환된 응답 DTO 객체
    //     → 이 객체가 JSON으로 변환되어 클라이언트에 전송됩니다.
    //
    // [static(정적) 메서드인 이유]
    //   ReviewResponse 객체를 아직 만들기 전에 호출해야 하므로
    //   인스턴스 없이도 호출할 수 있는 static 메서드로 선언합니다.
    //   호출 방법: ReviewResponse.from(review)
    //
    // [호출 예시 — ReviewService에서]
    //   List<Review> reviews = reviewRepository.findByRestaurantIdOrderByCreatedAtDesc(id);
    //   List<ReviewResponse> responses = reviews.stream()
    //       .map(ReviewResponse::from)     // 각 Review를 ReviewResponse로 변환
    //       .toList();                     // List로 수집
    // ──────────────────────────────────────────────────────
    public static ReviewResponse from(Review review) {

        // @Builder 패턴으로 ReviewResponse 객체를 하나씩 조립하여 생성합니다.
        return ReviewResponse.builder()

                // review.getId() → 리뷰의 고유 번호를 id 필드에 설정
                .id(review.getId())

                // review.getContent() → 리뷰 내용을 content 필드에 설정
                .content(review.getContent())

                // review.getRating() → 별점을 rating 필드에 설정
                .rating(review.getRating())

                // review.getMember().getNickname() → 작성자의 닉네임을 설정
                // Review → Member 연관관계를 통해 닉네임에 접근
                .memberNickname(review.getMember().getNickname())

                // review.getMember().getId() → 작성자의 회원 ID를 설정
                // 프론트엔드에서 "내 리뷰인지" 판별하는 데 사용됨
                .memberId(review.getMember().getId())

                // review.getRestaurant().getId() → 리뷰 대상 맛집의 ID를 설정
                // Review → Restaurant 연관관계를 통해 맛집 ID에 접근
                .restaurantId(review.getRestaurant().getId())

                // review.getCreatedAt() → 리뷰 작성 일시를 createdAt 필드에 설정
                .createdAt(review.getCreatedAt())

                // [Phase 7] review.getImageUrl() → 리뷰 이미지 URL을 설정 (null일 수 있음)
                .imageUrl(review.getImageUrl())

                // .build() → 위에서 설정한 모든 값으로 ReviewResponse 객체를 최종 생성
                .build();
    }
}