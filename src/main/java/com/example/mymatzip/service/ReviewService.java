package com.example.mymatzip.service;

import com.example.mymatzip.dto.PageResponse;
import com.example.mymatzip.dto.ReviewForm;
import com.example.mymatzip.dto.ReviewResponse;
import com.example.mymatzip.entity.Member;
import com.example.mymatzip.entity.Restaurant;
import com.example.mymatzip.entity.Review;
import com.example.mymatzip.entity.Role;
import com.example.mymatzip.repository.RestaurantRepository;
import com.example.mymatzip.repository.ReviewRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;

    public List<ReviewResponse> findByRestaurantId(Long restaurantId) {
        return reviewRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    public PageResponse<ReviewResponse> findByRestaurantId(Long restaurantId, Pageable pageable) {
        Page<Review> page = reviewRepository.findByRestaurantId(restaurantId, pageable);
        List<ReviewResponse> content = page.getContent().stream()
                .map(ReviewResponse::from)
                .toList();
        return PageResponse.from(page, content);
    }

    @Transactional
    public ReviewResponse save(ReviewForm form, Member loginMember) {
        if (loginMember == null) {
            throw new IllegalArgumentException("로그인이 필요한 서비스입니다.");
        }

        Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(form.getRestaurantId())
                .orElseThrow(() -> new EntityNotFoundException("맛집을 찾을 수 없습니다."));

        Review review = Review.builder()
                .content(form.getContent())
                .rating(form.getRating())
                .imageUrl(form.getImageUrl())
                .restaurant(restaurant)
                .member(loginMember)
                .build();

        restaurant.clearAiSummary();

        Review savedReview = reviewRepository.save(review);
        return ReviewResponse.from(savedReview);
    }

    @Transactional
    public void delete(Long id, Member loginMember) {
        if (loginMember == null) {
            throw new IllegalArgumentException("로그인이 필요한 서비스입니다.");
        }

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("리뷰를 찾을 수 없습니다."));

        boolean isOwner = review.getMember().getId().equals(loginMember.getId());
        boolean isAdmin = loginMember.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("삭제 권한이 없습니다.");
        }

        review.getRestaurant().clearAiSummary();
        reviewRepository.delete(review);
    }

    public Double getAverageRating(Long restaurantId) {
        Double avg = reviewRepository.averageRatingByRestaurantId(restaurantId);
        return avg != null ? Math.round(avg * 10) / 10.0 : 0.0;
    }

    @Transactional
    public void report(Long id, Member loginMember) {
        if (loginMember == null) {
            throw new IllegalArgumentException("로그인이 필요한 서비스입니다.");
        }

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("리뷰를 찾을 수 없습니다."));

        if (review.getMember().getId().equals(loginMember.getId())) {
            throw new IllegalArgumentException("본인의 리뷰는 신고할 수 없습니다.");
        }

        if (review.isReported()) {
            throw new IllegalArgumentException("이미 신고된 리뷰입니다.");
        }

        review.report();
    }
}