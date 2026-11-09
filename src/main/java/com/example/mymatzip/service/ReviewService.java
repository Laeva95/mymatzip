// ============================================================
// [ReviewService.java] 리뷰 비즈니스 로직 서비스
// ============================================================
// 역할: 리뷰(후기)와 관련된 핵심 업무를 처리합니다.
//       리뷰 작성, 삭제, 조회, 평균 평점 계산 등
//       Controller로부터 요청을 받아 → Repository를 통해 DB를 조작합니다.
//
// MVC 아키텍처에서의 위치:
//   Controller (요청 접수) → ★Service (업무 처리)★ → Repository (DB 접근)
//
//
// 사용처:
//   - ReviewController에서 리뷰 작성, 삭제, 조회 요청 시 호출
//   - 맛집 상세 페이지에서 해당 맛집의 리뷰 목록과 평균 평점을 보여줄 때 사용
//
// @Transactional 설명:
//   트랜잭션 = "하나의 작업 단위" (중간에 실패하면 전부 취소)
//   readOnly=true (기본): 조회 메서드에서 사용 → 성능 최적화
//   @Transactional (메서드 레벨): 등록/삭제처럼 DB에 변경이 필요한 경우 사용
// ============================================================

// 이 파일이 속한 패키지(폴더) 선언 — service 패키지에 위치함을 나타냄
package com.example.mymatzip.service;

// ─── import 영역 ─────────────────────────────────────────────

// [Phase 5] PageResponse: 페이징 응답 래퍼 DTO — 페이지 정보와 데이터 목록을 함께 전달
import com.example.mymatzip.dto.PageResponse;

// ReviewForm: 리뷰 작성 시 사용자가 입력한 정보를 담는 DTO(데이터 전달 객체)
//             (내용, 평점, 맛집 ID 등)
import com.example.mymatzip.dto.ReviewForm;

// ReviewResponse: 리뷰 정보를 화면(View)에 전달하기 위한 응답용 DTO
//                 (엔티티를 직접 노출하지 않고, 필요한 정보만 담아 전달)
import com.example.mymatzip.dto.ReviewResponse;

// Member: 회원 정보를 담는 엔티티 — 리뷰를 작성한 회원 정보를 참조하기 위해 사용
import com.example.mymatzip.entity.Member;

// Restaurant: 맛집 정보를 담는 엔티티 — 리뷰가 달린 맛집을 참조하기 위해 사용
import com.example.mymatzip.entity.Restaurant;

// Review: 리뷰 정보를 담는 엔티티 — DB의 review 테이블과 1:1 대응
import com.example.mymatzip.entity.Review;

// RestaurantRepository: DB의 restaurant 테이블에 접근하는 인터페이스
//                       → 리뷰 작성 시 맛집이 실제로 존재하는지 확인하기 위해 사용
import com.example.mymatzip.repository.RestaurantRepository;

// ReviewRepository: DB의 review 테이블에 접근하는 인터페이스 (CRUD 기능 제공)
//                   → 리뷰 저장, 조회, 삭제, 평균 평점 계산 등
import com.example.mymatzip.repository.ReviewRepository;

// @RequiredArgsConstructor: final 필드에 대한 생성자를 자동 생성해주는 Lombok 어노테이션
// → 직접 생성자를 작성하지 않아도 의존성 주입(DI)이 자동으로 이루어짐
import lombok.RequiredArgsConstructor;

// @Service: 이 클래스가 "서비스 계층"임을 Spring에게 알려주는 어노테이션
// → Spring이 자동으로 이 클래스의 객체(Bean)를 생성하고 관리함
import org.springframework.stereotype.Service;

// @Transactional: DB 작업을 하나의 트랜잭션(작업 단위)으로 묶어주는 어노테이션
// → 작업 도중 에러 발생 시 모든 변경사항을 자동으로 되돌림 (rollback)
import org.springframework.transaction.annotation.Transactional;

// [Phase 5] Page, Pageable 가져오기 — 페이징 기능을 위한 클래스
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// [팀 프로젝트] 입력값은 원문 그대로 저장하고, 화면에 출력할 때 타임리프 th:text 와 common.js 의 esc() 가 HTML 이스케이프합니다.

// List: 여러 개의 데이터를 순서대로 담을 수 있는 자바 컬렉션 (목록)
// → 리뷰 목록을 담아 반환할 때 사용
import java.util.List;

// ─── 클래스 선언부 ────────────────────────────────────────────

// @Service: Spring에게 "이 클래스는 비즈니스 로직을 담당하는 서비스입니다"라고 등록
//           → Spring 컨테이너가 이 클래스의 인스턴스를 자동 생성·관리함
@Service

// @RequiredArgsConstructor: 아래의 final 필드들(reviewRepository, restaurantRepository)을
//                           매개변수로 받는 생성자를 Lombok이 자동 생성
//                           → Spring이 생성자를 통해 각 Repository를 자동 주입(DI)
@RequiredArgsConstructor

// @Transactional(readOnly = true): 이 클래스의 모든 메서드에 기본적으로 "읽기 전용" 트랜잭션 적용
//   - readOnly = true란? → DB에서 데이터를 "읽기만" 하겠다는 의미
//   - 장점: DB가 불필요한 잠금(Lock)을 걸지 않아 성능이 향상됨
//   - 쓰기가 필요한 메서드에는 별도로 @Transactional을 붙여 readOnly를 해제함
@Transactional(readOnly = true)
public class ReviewService {

    // ─── 필드(의존성) 영역 ────────────────────────────────────

    // reviewRepository: DB의 review 테이블에 접근하기 위한 저장소 객체
    // final 키워드: 한 번 주입된 후 변경 불가 → 안전성 보장
    // 역할: 리뷰 데이터의 조회, 저장, 삭제, 평균 평점 계산을 담당
    private final ReviewRepository reviewRepository;

    // restaurantRepository: DB의 restaurant 테이블에 접근하기 위한 저장소 객체
    // 역할: 리뷰 작성 시 대상 맛집이 실제로 존재하는지 확인하기 위해 사용
    //       (존재하지 않는 맛집에 리뷰를 달 수 없으므로)
    private final RestaurantRepository restaurantRepository;

    // ─── 맛집별 리뷰 목록 조회 메서드 ─────────────────────────

    // @Transactional이 없음 → 클래스 레벨의 readOnly = true 적용
    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: findByRestaurantId (맛집 ID로 리뷰 목록 조회)               │
    // │ 매개변수: Long restaurantId — 리뷰를 조회할 맛집의 고유번호             │
    // │ 리턴값:  List<ReviewResponse> — 해당 맛집의 리뷰 응답 DTO 목록         │
    // │ 동작 흐름:                                                          │
    // │   1단계: DB에서 해당 맛집의 리뷰를 최신순으로 조회                      │
    // │   2단계: 각 리뷰 엔티티를 응답 DTO로 변환                             │
    // │   3단계: 변환된 DTO 목록을 반환                                      │
    // │ 사용처: 맛집 상세 페이지에서 리뷰 목록을 보여줄 때 사용                  │
    // └─────────────────────────────────────────────────────────────────────┘
    public List<ReviewResponse> findByRestaurantId(Long restaurantId) {
        // reviewRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId):
        //   → 특정 맛집(restaurantId)에 달린 리뷰들을 생성일(createdAt) 내림차순(최신순)으로 조회
        //   → Spring Data JPA의 메서드 이름 규칙으로 자동 생성된 쿼리
        // .stream(): 리스트를 스트림(흐름)으로 변환 → 데이터를 하나씩 처리할 수 있게 함
        // .map(ReviewResponse::from): 각 Review 엔티티를 ReviewResponse DTO로 변환
        //   → ReviewResponse.from()은 정적 팩토리 메서드 (엔티티 → DTO 변환 담당)
        // .toList(): 스트림의 결과를 다시 List(목록)로 변환하여 반환
        return reviewRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    // ─── [Phase 5] 맛집별 리뷰 페이징 조회 메서드 ──────────────

    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: findByRestaurantId (맛집별 리뷰 페이징 조회)                │
    // │ 매개변수: Long restaurantId — 리뷰를 조회할 맛집의 고유번호            │
    // │          Pageable pageable — 페이지 번호와 크기 정보                 │
    // │ 리턴값:  PageResponse<ReviewResponse> — 페이징 정보 + 리뷰 DTO       │
    // │ 동작 흐름:                                                          │
    // │   1단계: DB에서 해당 맛집의 리뷰를 해당 페이지만큼 조회                 │
    // │   2단계: 각 리뷰 엔티티를 응답 DTO로 변환                             │
    // │   3단계: PageResponse로 감싸서 반환                                  │
    // └─────────────────────────────────────────────────────────────────────┘
    public PageResponse<ReviewResponse> findByRestaurantId(Long restaurantId, Pageable pageable) {
        Page<Review> page = reviewRepository.findByRestaurantId(restaurantId, pageable);
        List<ReviewResponse> content = page.getContent().stream()
                .map(ReviewResponse::from)
                .toList();
        return PageResponse.from(page, content);
    }

    // ─── 리뷰 작성(저장) 메서드 ───────────────────────────────

    // @Transactional: 이 메서드는 DB에 데이터를 "쓰는" 작업이므로
    //                 클래스 레벨의 readOnly=true를 덮어씀 (쓰기 가능 트랜잭션)
    //                 → 메서드 실행 중 오류 발생 시 모든 DB 변경이 자동 취소(rollback)됨
    @Transactional
    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: save (리뷰 작성)                                           │
    // │ 매개변수: ReviewForm form — 사용자가 입력한 리뷰 정보                   │
    // │          (내용, 평점, 맛집ID가 담겨 있음)                              │
    // │          Member loginMember — 현재 로그인한 회원 (리뷰 작성자)          │
    // │ 리턴값:  ReviewResponse — 작성된 리뷰의 응답 DTO                      │
    // │ 동작 흐름:                                                          │
    // │   1단계: 리뷰 대상 맛집이 존재하는지 확인 (없으면 예외)                  │
    // │   2단계: Review 엔티티 객체 생성 (빌더 패턴)                           │
    // │   3단계: DB에 저장 후 응답 DTO로 변환하여 반환                         │
    // └─────────────────────────────────────────────────────────────────────┘
    public ReviewResponse save(ReviewForm form, Member loginMember) {

        // [1단계] 리뷰 대상 맛집이 존재하는지 확인
        // restaurantRepository.findById(): DB에서 해당 ID의 맛집을 조회
        // form.getRestaurantId(): 리뷰 폼에 담긴 맛집 ID (어떤 맛집에 리뷰를 다는지)
        // .orElseThrow(): 맛집이 존재하지 않으면 예외 발생
        // → 존재하지 않는 맛집에는 리뷰를 작성할 수 없으므로 사전 확인
        Restaurant restaurant = restaurantRepository.findById(form.getRestaurantId())
                .orElseThrow(() -> new IllegalArgumentException("맛집을 찾을 수 없습니다."));

        // [2단계] Review 엔티티 객체 생성 (빌더 패턴 사용)
        // 빌더 패턴: .속성명(값) 형태로 가독성 좋게 객체를 생성하는 디자인 패턴
        Review review = Review.builder()
                // .content(): 사용자가 입력한 리뷰 내용 (텍스트) 설정
                // 원문 그대로 저장 (XSS 방어는 출력할 때: 타임리프 th:text, common.js esc())
                .content(form.getContent())
                // .rating(): 사용자가 선택한 평점 설정 (예: 1~5점)
                .rating(form.getRating())
                // .member(): 리뷰를 작성한 회원 정보 설정 → 누가 작성했는지 기록
                .member(loginMember)
                // .restaurant(): 리뷰 대상 맛집 정보 설정 → 어떤 맛집에 대한 리뷰인지 기록
                .restaurant(restaurant)
                // [Phase 7] .imageUrl(): 리뷰 이미지 URL 설정 (선택값)
                .imageUrl(form.getImageUrl())
                // .build(): 위에서 설정한 값들로 최종 Review 객체를 생성
                .build();

        // [3단계] DB에 리뷰 저장 후 응답 DTO로 변환하여 반환
        // reviewRepository.save(review): DB의 review 테이블에 INSERT(삽입)
        // ReviewResponse.from(): 저장된 Review 엔티티를 ReviewResponse DTO로 변환
        // → 엔티티를 직접 노출하지 않고, 화면에 필요한 정보만 담아서 반환
        // [팀 프로젝트] 리뷰가 늘었으므로 저장된 AI 요약을 비운다 (변경 감지로 UPDATE restaurant 실행)
        restaurant.clearAiSummary();
        return ReviewResponse.from(reviewRepository.save(review));
    }

    // ─── 리뷰 삭제 메서드 ─────────────────────────────────────

    // @Transactional: DB에서 데이터를 "삭제"하는 작업이므로 쓰기 가능 트랜잭션 적용
    @Transactional
    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: delete (리뷰 삭제)                                         │
    // │ 매개변수: Long id — 삭제할 리뷰의 고유번호                             │
    // │          Member loginMember — 현재 로그인한 회원 (권한 확인용)          │
    // │ 리턴값:  void (없음) — 삭제 작업이므로 반환할 데이터 없음               │
    // │ 동작 흐름:                                                          │
    // │   1단계: ID로 리뷰 조회 (없으면 예외)                                 │
    // │   2단계: 삭제 권한 확인 (작성자 본인만 삭제 가능)                       │
    // │   3단계: DB에서 리뷰 삭제                                            │
    // └─────────────────────────────────────────────────────────────────────┘
    public void delete(Long id, Member loginMember) {

        // [1단계] ID로 리뷰 조회
        // reviewRepository.findById(id): DB에서 해당 ID의 리뷰를 조회
        // .orElseThrow(): 리뷰가 존재하지 않으면 예외 발생
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        // [2단계] 삭제 권한 확인
        // review.getMember().getId(): 리뷰를 작성한 회원의 ID
        // loginMember.getId(): 현재 로그인한 회원의 ID
        // !...equals(...): 두 ID가 같지 않으면 → 작성자가 아니므로 삭제 권한 없음
        if (!review.getMember().getId().equals(loginMember.getId())) {
            // 작성자가 아닌 사람이 삭제하려고 하면 예외 발생
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        // [3단계] DB에서 리뷰 삭제
        // reviewRepository.delete(review): DB에서 해당 리뷰 데이터를 DELETE(삭제)
        // [팀 프로젝트] 리뷰가 줄었으므로 저장된 AI 요약을 비운다
        review.getRestaurant().clearAiSummary();
        reviewRepository.delete(review);
    }

    // ─── 평균 평점 조회 메서드 ────────────────────────────────

    // @Transactional이 없음 → 클래스 레벨의 readOnly = true 적용
    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: getAverageRating (맛집의 평균 평점 조회)                     │
    // │ 매개변수: Long restaurantId — 평균 평점을 조회할 맛집의 고유번호         │
    // │ 리턴값:  Double — 평균 평점 (소수점 첫째 자리까지, 예: 4.3)             │
    // │ 동작 흐름:                                                          │
    // │   1단계: DB에서 해당 맛집의 평균 평점 계산                             │
    // │   2단계: 소수점 첫째 자리까지 반올림 (null이면 0.0 반환)                │
    // │ 사용처: 맛집 상세 페이지에서 평균 평점을 표시할 때 사용                  │
    // └─────────────────────────────────────────────────────────────────────┘
    public Double getAverageRating(Long restaurantId) {

        // reviewRepository.averageRatingByRestaurantId(): DB에서 해당 맛집 리뷰들의 평균 평점 계산
        // → SQL: SELECT AVG(rating) FROM review WHERE restaurant_id = ?
        // → 리뷰가 하나도 없으면 null이 반환됨
        Double avg = reviewRepository.averageRatingByRestaurantId(restaurantId);

        // 삼항 연산자를 사용한 null 처리 및 반올림
        // avg != null ? (참일 때 실행) : (거짓일 때 실행)
        //   - avg가 null이 아니면: Math.round(avg * 10) / 10.0 → 소수점 첫째 자리까지 반올림
        //     예: avg = 4.267 → 4.267 * 10 = 42.67 → Math.round(42.67) = 43 → 43 / 10.0 = 4.3
        //   - avg가 null이면: 0.0을 반환 (리뷰가 없는 맛집은 평점 0.0)
        return avg != null ? Math.round(avg * 10) / 10.0 : 0.0;
    }

    // ─── 리뷰 신고 메서드 ───────────────────────────────────

    @Transactional
    public void report(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
        review.report();
    }
}