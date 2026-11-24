// [C수정] AI 판정 트랜잭션 경계 및 리뷰 저장 흐름 수정
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
import com.example.mymatzip.service.ai.AiReviewSpamDetectionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    // [추가] 리뷰에 사용할 수 있는 사전 정의 키워드
    private static final Set<String> ALLOWED_REVIEW_KEYWORDS = Set.of(
            "음식이 맛있어요", "가성비가 좋아요", "분위기가 좋아요", "친절해요",
            "양이 많아요", "매장이 깨끗해요", "혼밥하기 좋아요", "재방문하고 싶어요"
    );

    private final ReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;
    private final FileService fileService;
    // AI 리뷰 스팸 판별 서비스
    private final AiReviewSpamDetectionService aiReviewSpamDetectionService;

    // AI 호출이 끝난 뒤 저장 구간에서만 트랜잭션을 연다.
    private TransactionTemplate transactionTemplate;
    private TransactionTemplate aiOutsideTransactionTemplate;

    @Autowired
    void configureTransactionTemplate(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.aiOutsideTransactionTemplate = new TransactionTemplate(transactionManager);
        this.aiOutsideTransactionTemplate.setPropagationBehavior(
                org.springframework.transaction.TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
    }

    public List<ReviewResponse> findByRestaurantId(Long restaurantId) {
        return findByRestaurantId(restaurantId, (Member) null);
    }

    // [추가] 로그인 회원의 도움돼요 여부를 포함한 리뷰 목록 조회
    public List<ReviewResponse> findByRestaurantId(Long restaurantId, Member loginMember) {
        Long loginMemberId = loginMember != null ? loginMember.getId() : null;
        return reviewRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId).stream()
                .map(review -> ReviewResponse.from(review, loginMemberId))
                .toList();
    }

    public PageResponse<ReviewResponse> findByRestaurantId(Long restaurantId, Pageable pageable) {
        return findByRestaurantId(restaurantId, pageable, null);
    }

    // [추가] 페이징 리뷰 목록에도 로그인 회원의 도움돼요 여부 포함
    public PageResponse<ReviewResponse> findByRestaurantId(
            Long restaurantId, Pageable pageable, Member loginMember) {
        Page<Review> page = reviewRepository.findByRestaurantId(restaurantId, pageable);
        Long loginMemberId = loginMember != null ? loginMember.getId() : null;
        List<ReviewResponse> content = page.getContent().stream()
                .map(review -> ReviewResponse.from(review, loginMemberId))
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
        // [추가] 자기 가게에는 리뷰를 쓸 수 없다
        if (restaurant.getMember().getId().equals(loginMember.getId())) {
            throw new AccessDeniedException("본인 가게에는 리뷰를 작성할 수 없습니다.");
        }
        // [추가] 전달된 키워드가 허용 목록에 있는지 확인
        List<String> selectedKeywords = form.getKeywordTags() == null
                ? List.of() : form.getKeywordTags();

        if (selectedKeywords.size() > 3) {
            throw new IllegalArgumentException("키워드는 최대 3개까지 선택할 수 있습니다.");
        }

        for (String keyword : selectedKeywords) {
            if (keyword == null || !ALLOWED_REVIEW_KEYWORDS.contains(keyword)) {
                throw new IllegalArgumentException("올바르지 않은 리뷰 키워드입니다.");
            }
        }

        // 명확한 스팸(BLOCK)은 DB에 저장하지 않는다.
        // 애매한 스팸(SPAM)만 저장 후 관리자 의심 목록에 남긴다.
        AiReviewSpamDetectionService.Result spamResult =
                detectOutsideTransaction(form.getContent());
        if (spamResult.blocked()) {
            throw new IllegalArgumentException("스팸으로 확정된 리뷰는 등록할 수 없습니다. "
                    + spamResult.reason());
        }

        return persistReview(form, selectedKeywords, loginMember, restaurant, spamResult);
    }

    private AiReviewSpamDetectionService.Result detectOutsideTransaction(String content) {
        if (aiOutsideTransactionTemplate == null) {
            return aiReviewSpamDetectionService.detect(content);
        }
        return aiOutsideTransactionTemplate.execute(status -> aiReviewSpamDetectionService.detect(content));
    }

    private ReviewResponse persistReview(ReviewForm form, List<String> selectedKeywords,
                                         Member loginMember,
                                         Restaurant restaurant,
                                         AiReviewSpamDetectionService.Result spamResult) {
        java.util.function.Supplier<ReviewResponse> operation = () -> {
            Restaurant managedRestaurant = restaurant;
            if (transactionTemplate != null) {
                managedRestaurant = restaurantRepository.findByIdAndDeletedAtIsNull(form.getRestaurantId())
                        .orElse(restaurant);
            }
            managedRestaurant.clearAiSummary();

            Review review = Review.builder()
                    .content(form.getContent())
                    .rating(form.getRating())
                    .imageUrl(form.getImageUrl())
                    .restaurant(managedRestaurant)
                    .member(loginMember)
                    .build();
            review.changeKeywordTags(new LinkedHashSet<>(selectedKeywords));

            Review savedReview = reviewRepository.save(review);
            if (spamResult.spam()) {
                savedReview.reportAsSpam(spamResult.reason());
            }
            return ReviewResponse.from(savedReview, loginMember.getId());
        };

        // 단위 테스트처럼 스프링 컨테이너 밖에서 호출되는 경우에도 동작하도록 직접 실행한다.
        return transactionTemplate == null ? operation.get() : transactionTemplate.execute(status -> operation.get());
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
        // [추가] 리뷰 이미지 기록 삭제
        if (review.getImageUrl() != null) {
            fileService.deleteImageRecord(review.getImageUrl());
        }
        reviewRepository.delete(review);
    }

    public Double getAverageRating(Long restaurantId) {
        Double avg = reviewRepository.averageRatingByRestaurantId(restaurantId);
        return avg != null ? Math.round(avg * 10) / 10.0 : 0.0;
    }

    // [추가] 리뷰 도움돼요 등록
    @Transactional
    public ReviewResponse addHelpful(Long id, Member loginMember) {
        if (loginMember == null) {
            throw new IllegalArgumentException("로그인이 필요한 서비스입니다.");
        }

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("리뷰를 찾을 수 없습니다."));

        if (review.getMember().getId().equals(loginMember.getId())) {
            throw new IllegalArgumentException("본인의 리뷰에는 도움돼요를 누를 수 없습니다.");
        }

        if (!review.addHelpful(loginMember.getId())) {
            throw new IllegalArgumentException("이미 도움돼요를 눌렀습니다.");
        }

        return ReviewResponse.from(review, loginMember.getId());
    }

    // [추가] 리뷰 도움돼요 취소
    @Transactional
    public ReviewResponse removeHelpful(Long id, Member loginMember) {
        if (loginMember == null) {
            throw new IllegalArgumentException("로그인이 필요한 서비스입니다.");
        }

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("리뷰를 찾을 수 없습니다."));

        review.removeHelpful(loginMember.getId());
        return ReviewResponse.from(review, loginMember.getId());
    }

    // [추가] 맛집별 인기 키워드 조회
    public List<ReviewRepository.KeywordCount> getPopularKeywords(Long restaurantId) {
        return reviewRepository.findPopularKeywordsByRestaurantId(restaurantId);
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
