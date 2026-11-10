// ============================================================
// [PageResponse.java] 페이징 응답 래퍼 DTO
// ============================================================
// 역할: 페이지 정보와 데이터 목록을 함께 클라이언트에 전달하는 "포장 상자"
//       Spring의 Page 객체를 그대로 보내면 불필요한 내부 정보까지 노출되므로,
//       필요한 정보만 골라서 이 DTO에 담아 전달합니다.
//
//
// 제네릭(Generic) <T>란?
//   "어떤 타입이든 담을 수 있는 만능 상자"입니다.
//   T 자리에 RestaurantResponse를 넣으면 → PageResponse<RestaurantResponse>
//   T 자리에 ReviewResponse를 넣으면 → PageResponse<ReviewResponse>
//
// 사용 흐름:
//   1) Service에서 Repository의 Page<Entity> 결과를 받음
//   2) Entity → Response DTO로 변환한 List를 만듦
//   3) PageResponse.from(page, dtoList)로 페이징 정보 + 데이터를 합쳐서 반환
//   4) Controller가 JSON으로 변환하여 클라이언트에 전송
//
// 사용되는 곳:
//   - RestaurantService (맛집 목록 페이징 조회)
//   - ReviewService (리뷰 목록 페이징 조회)
// ============================================================

// --- 패키지 선언 ---
package com.example.mymatzip.dto;

// --- import 영역 ---

// @Builder 가져오기 — Lombok이 빌더 패턴 코드를 자동 생성
import lombok.Builder;

// @Getter 가져오기 — Lombok이 모든 필드의 getter 메서드를 자동 생성
import lombok.Getter;

// Page 가져오기 — Spring Data JPA가 제공하는 페이징 결과 객체
// from() 메서드에서 Page 객체의 페이징 정보를 추출하기 위해 사용
import org.springframework.data.domain.Page;

// List 가져오기 — 현재 페이지의 데이터를 담는 목록 자료구조
import java.util.List;

// @Getter: 모든 필드에 대해 getter 메서드를 자동 생성
// Spring이 이 객체를 JSON으로 변환할 때 getter를 호출합니다.
@Getter

// @Builder: 빌더 패턴 코드를 자동 생성
// PageResponse.builder().content(...).page(...).build() 형태로 객체를 생성할 수 있음
@Builder
public class PageResponse<T> {

    // ──────────────────────────────────────────────────────
    // 현재 페이지의 데이터 목록
    // ──────────────────────────────────────────────────────
    // 예: [맛집1, 맛집2, 맛집3, ... 맛집10] (한 페이지에 10개)
    private List<T> content;

    // ──────────────────────────────────────────────────────
    // 현재 페이지 번호 (0부터 시작)
    // ──────────────────────────────────────────────────────
    // 주의: 0부터 시작! (0 = 첫 번째 페이지, 1 = 두 번째 페이지)
    // 프론트엔드에서는 보통 +1 해서 "1페이지, 2페이지..."로 표시
    private int page;

    // ──────────────────────────────────────────────────────
    // 한 페이지당 데이터 수
    // ──────────────────────────────────────────────────────
    // 예: size=10이면 한 페이지에 맛집 10개씩 표시
    private int size;

    // ──────────────────────────────────────────────────────
    // 전체 데이터 수
    // ──────────────────────────────────────────────────────
    // long 타입인 이유: 데이터가 int 범위(약 21억)를 넘을 수 있으므로
    private long totalElements;

    // ──────────────────────────────────────────────────────
    // 전체 페이지 수
    // ──────────────────────────────────────────────────────
    // 계산: totalElements / size (올림)
    // 예: 총 157건, 한 페이지 10개 → 16페이지
    private int totalPages;

    // ──────────────────────────────────────────────────────
    // 첫 번째 페이지인지 여부
    // ──────────────────────────────────────────────────────
    // true이면 "이전" 버튼을 비활성화하는 데 사용
    private boolean first;

    // ──────────────────────────────────────────────────────
    // 마지막 페이지인지 여부
    // ──────────────────────────────────────────────────────
    // true이면 "다음" 버튼을 비활성화하는 데 사용
    private boolean last;

    // ──────────────────────────────────────────────────────
    // Page → PageResponse 변환 정적 팩토리 메서드
    // ──────────────────────────────────────────────────────
    // [역할] Spring의 Page 객체에서 페이징 정보를 추출하고,
    //        변환된 DTO 목록(content)과 합쳐서 PageResponse를 생성합니다.
    //
    // [매개변수]
    //   - Page<?> page   : Spring Data JPA가 반환한 페이징 결과 객체
    //     → 페이지 번호, 전체 개수, 총 페이지 수 등의 메타 정보를 담고 있음
    //   - List<T> content : Entity → DTO로 변환된 데이터 목록
    //     → Service에서 미리 변환해둔 DTO 리스트를 받습니다
    //
    // [반환값]
    //   - PageResponse<T> : 페이징 정보 + 데이터가 합쳐진 응답 객체
    //
    // [호출 예시 — RestaurantService에서]
    //   Page<Restaurant> page = restaurantRepository.findAll(pageable);
    //   List<RestaurantResponse> dtoList = page.getContent().stream()
    //       .map(this::toResponse).toList();
    //   return PageResponse.from(page, dtoList);
    // ──────────────────────────────────────────────────────
    public static <T> PageResponse<T> from(Page<?> page, List<T> content) {
        return PageResponse.<T>builder()
                .content(content)                       // 변환된 DTO 데이터 목록
                .page(page.getNumber())                 // 현재 페이지 번호 (0부터)
                .size(page.getSize())                   // 한 페이지당 데이터 수
                .totalElements(page.getTotalElements()) // 전체 데이터 수
                .totalPages(page.getTotalPages())       // 전체 페이지 수
                .first(page.isFirst())                  // 첫 번째 페이지 여부
                .last(page.isLast())                    // 마지막 페이지 여부
                .build();
    }
}