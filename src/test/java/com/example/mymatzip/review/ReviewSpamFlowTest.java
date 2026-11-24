package com.example.mymatzip.review;

import com.example.mymatzip.dto.ReviewForm;
import com.example.mymatzip.entity.Member;
import com.example.mymatzip.entity.Restaurant;
import com.example.mymatzip.entity.Review;
import com.example.mymatzip.repository.RestaurantRepository;
import com.example.mymatzip.repository.ReviewRepository;
import com.example.mymatzip.service.FileService;
import com.example.mymatzip.service.ReviewService;
import com.example.mymatzip.service.ai.AiReviewSpamDetectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewSpamFlowTest {
    @Mock ReviewRepository reviewRepository;
    @Mock RestaurantRepository restaurantRepository;
    @Mock FileService fileService;
    @Mock AiReviewSpamDetectionService spamDetectionService;

    @Test
    void 스팸_판정이면_리뷰가_관리자_신고목록에_자동등록된다() {
        Member writer = mock(Member.class);
        Member owner = mock(Member.class);
        Restaurant restaurant = mock(Restaurant.class);
        when(writer.getId()).thenReturn(2L);
        when(writer.getNickname()).thenReturn("작성자");
        when(owner.getId()).thenReturn(1L);
        when(restaurant.getId()).thenReturn(10L);
        when(restaurant.getMember()).thenReturn(owner);
        when(restaurantRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(restaurant));
        when(spamDetectionService.detect("무료 쿠폰은 example.com에서 신청하세요")).thenReturn(
                new AiReviewSpamDetectionService.Result(true, "외부 링크를 홍보하는 내용입니다."));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewService service = new ReviewService(reviewRepository, restaurantRepository, fileService, spamDetectionService);
        ReviewForm form = new ReviewForm();
        form.setRestaurantId(10L);
        form.setRating(1);
        form.setContent("무료 쿠폰은 example.com에서 신청하세요");

        service.save(form, writer);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review saved = captor.getValue();
        assertThat(saved.isReported()).isTrue();
        assertThat(saved.isAiSpam()).isTrue();
        assertThat(saved.getAiSpamReason()).isEqualTo("외부 링크를 홍보하는 내용입니다.");
    }

    @Test
    void 확정_스팸이면_리뷰를_저장하지_않는다() {
        Member writer = mock(Member.class);
        Member owner = mock(Member.class);
        Restaurant restaurant = mock(Restaurant.class);
        when(writer.getId()).thenReturn(2L);
        when(owner.getId()).thenReturn(1L);
        when(restaurant.getMember()).thenReturn(owner);
        when(restaurantRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(restaurant));
        when(spamDetectionService.detect("카지노 가입 시 첫 충전 보너스 이벤트")).thenReturn(
                new AiReviewSpamDetectionService.Result(true, true, "도박 사이트 가입을 유도하는 명백한 광고입니다."));

        ReviewService service = new ReviewService(reviewRepository, restaurantRepository, fileService, spamDetectionService);
        ReviewForm form = new ReviewForm();
        form.setRestaurantId(10L);
        form.setRating(5);
        form.setContent("카지노 가입 시 첫 충전 보너스 이벤트");

        assertThatThrownBy(() -> service.save(form, writer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("스팸으로 확정된 리뷰");
        verify(reviewRepository, never()).save(any(Review.class));
    }
}
