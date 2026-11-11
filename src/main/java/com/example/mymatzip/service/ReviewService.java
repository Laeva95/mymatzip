package com.example.mymatzip.service;

// ─── import 영역 ─────────────────────────────────────────────

import com.example.mymatzip.dto.PageResponse;
import com.example.mymatzip.dto.ReviewForm;
import com.example.mymatzip.dto.ReviewResponse;
import com.example.mymatzip.entity.Member;
import com.example.mymatzip.entity.Restaurant;
import com.example.mymatzip.entity.Review;
import com.example.mymatzip.repository.RestaurantRepository;
import com.example.mymatzip.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// [팀 프로젝트] 입력값은 원문 그대로 저장하고, 화면에 출력할 때 타임리프 th:text 와 common.js 의 esc() 가 HTML 이스케이프합니다.

// ─── 클래스 선언부 ────────────────────────────────────────────

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    // ─── 필드(의존성) 영역 ────────────────────────────────────

    private final ReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;

    // ─── 맛집별 리뷰 목록 조회 메서드 (전체) ──────────────────

    public List<ReviewResponse> findByRestaurantId(Long restaurantId) {
        return reviewRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    // ─── [Phase 5] 맛집별 리뷰 페이징 조회 메서드 ──────────────

    public PageResponse<ReviewResponse> findByRestaurantId(Long restaurantId, Pageable pageable) {
        Page<Review> page = reviewRepository.findByRestaurantId(restaurantId, pageable);
        List<ReviewResponse> content = page.getContent().stream()
                .map(ReviewResponse::from)
                .toList();
        return PageResponse.from(page, content);
    }

    // ─── 리뷰 작성(저장) 메서드 ───────────────────────────────

    @Transactional
    public ReviewResponse save(ReviewForm form, Member loginMember) {

        // [1단계] 리뷰 대상 맛집이 존재하는지 확인
        Restaurant restaurant = restaurantRepository.findById(form.getRestaurantId())
                .orElseThrow(() -> new IllegalArgumentException("맛집을 찾을 수 없습니다."));

        // [2단계] Review 엔티티 객체 생성 (빌더 패턴 사용)
        Review review = Review.builder()
                .content(form.getContent())
                .rating(form.getRating())
                .imageUrl(form.getImageUrl())
                .restaurant(restaurant)
                .member(loginMember)
                .build();

        // [팀 프로젝트] 새 리뷰가 추가되었으므로 기존 AI 요약을 비워 최신화가 가능하도록 함
        restaurant.clearAiSummary();

        // [3단계] DB에 저장 후 응답 DTO로 변환하여 반환
        Review savedReview = reviewRepository.save(review);
        return ReviewResponse.from(savedReview);
    }

    // ─── 리뷰 삭제 메서드 ─────────────────────────────────────

    @Transactional
    public void delete(Long id, Member loginMember) {

        // [1단계] ID로 삭제 대상 리뷰 조회
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."
                ));

        // [2단계] 삭제 권한 확인
        if (!review.getMember().getId().equals(loginMember.getId())) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        // [3단계] DB에서 리뷰 삭제
        // [팀 프로젝트] 리뷰가 줄었으므로 저장된 AI 요약을 비운다
        review.getRestaurant().clearAiSummary();
        reviewRepository.delete(review);
    }

    // ─── 평균 평점 조회 메서드 ────────────────────────────────

    public Double getAverageRating(Long restaurantId) {

        // DB에서 해당 맛집 리뷰들의 평균 평점 계산 (리뷰가 없으면 null 반환)
        Double avg = reviewRepository.averageRatingByRestaurantId(restaurantId);

        // null 처리 및 소수점 첫째 자리 반올림 (리뷰가 없는 맛집은 0.0 반환)
        return avg != null ? Math.round(avg * 10) / 10.0 : 0.0;
    }

    // ─── 리뷰 신고 메서드 ─────────────────────────────────────

    @Transactional
    public void report(Long id, Member loginMember) {

        // [1단계] ID로 신고 대상 리뷰 조회
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."
                ));

        // [2단계] 본인 리뷰 신고 차단
        if (review.getMember().getId().equals(loginMember.getId())) {
            throw new IllegalArgumentException("본인의 리뷰는 신고할 수 없습니다.");
        }

        // [3단계] 이미 신고된 리뷰인지 확인
        if (review.isReported()) {
            throw new IllegalArgumentException("이미 신고된 리뷰입니다.");
        }

        // [4단계] 리뷰 신고 상태 변경
        // (트랜잭션 종료 시 JPA 더티 체킹에 의해 자동으로 UPDATE 쿼리 실행)
        review.report();
    }
}