package com.example.mymatzip.review;

import com.example.mymatzip.dto.ReviewForm;
import com.example.mymatzip.entity.Member;
import com.example.mymatzip.entity.Restaurant;
import com.example.mymatzip.entity.Review;
import com.example.mymatzip.entity.ReviewModerationStatus;
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
    void 의심_리뷰는_Pending_상태로_저장하고_관리자_신고는_배치_검토_후_등록한다() {
        String content = "문의는 카톡으로 주세요. example.com";
        Member writer = mock(Member.class);
        Member owner = mock(Member.class);
        Restaurant restaurant = mock(Restaurant.class);

        when(writer.getId()).thenReturn(2L);
        when(writer.getNickname()).thenReturn("작성자");
        when(owner.getId()).thenReturn(1L);
        when(restaurant.getId()).thenReturn(10L);
        when(restaurant.getMember()).thenReturn(owner);
        when(restaurantRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(restaurant));
        when(spamDetectionService.classifyForRegistration(content)).thenReturn(
                new AiReviewSpamDetectionService.Result(
                        true, false, "연락 수단이 포함되어 AI 검토 후 확인됩니다."));
        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReviewService service = new ReviewService(
                reviewRepository, restaurantRepository, fileService, spamDetectionService);
        ReviewForm form = createForm(content);

        service.save(form, writer);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review saved = captor.getValue();

        assertThat(saved.getModerationStatus()).isEqualTo(ReviewModerationStatus.PENDING);
        assertThat(saved.isReported()).isFalse();
        assertThat(saved.isAiSpam()).isFalse();
        verify(spamDetectionService).classifyForRegistration(content);
        verify(spamDetectionService, never()).detect(anyString());
    }

    @Test
    void 정상_리뷰는_Normal_상태로_저장한다() {
        String content = "음식이 맛있고 직원분들이 친절했습니다.";
        Member writer = mock(Member.class);
        Member owner = mock(Member.class);
        Restaurant restaurant = mock(Restaurant.class);

        when(writer.getId()).thenReturn(2L);
        when(writer.getNickname()).thenReturn("작성자");
        when(owner.getId()).thenReturn(1L);
        when(restaurant.getId()).thenReturn(10L);
        when(restaurant.getMember()).thenReturn(owner);
        when(restaurantRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(restaurant));
        when(spamDetectionService.classifyForRegistration(content)).thenReturn(
                new AiReviewSpamDetectionService.Result(
                        false, false, "광고로 이어질 연락 수단이 없어 정상으로 등록했습니다."));
        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReviewService service = new ReviewService(
                reviewRepository, restaurantRepository, fileService, spamDetectionService);

        service.save(createForm(content), writer);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review saved = captor.getValue();

        assertThat(saved.getModerationStatus()).isEqualTo(ReviewModerationStatus.NORMAL);
        assertThat(saved.isReported()).isFalse();
        assertThat(saved.isAiSpam()).isFalse();
        verify(spamDetectionService).classifyForRegistration(content);
        verify(spamDetectionService, never()).detect(anyString());
    }

    @Test
    void 확정_스팸은_리뷰를_저장하지_않는다() {
        String content = "카지노 가입 시 첫 충전 보너스 이벤트";
        Member writer = mock(Member.class);
        Member owner = mock(Member.class);
        Restaurant restaurant = mock(Restaurant.class);

        when(writer.getId()).thenReturn(2L);
        when(owner.getId()).thenReturn(1L);
        when(restaurant.getMember()).thenReturn(owner);
        when(restaurantRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(restaurant));
        when(spamDetectionService.classifyForRegistration(content)).thenReturn(
                new AiReviewSpamDetectionService.Result(
                        true, true, "명백한 광고성 스팸 패턴이 포함되어 등록을 차단했습니다."));

        ReviewService service = new ReviewService(
                reviewRepository, restaurantRepository, fileService, spamDetectionService);

        assertThatThrownBy(() -> service.save(createForm(content), writer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("스팸으로 확정된 리뷰");

        verify(reviewRepository, never()).save(any(Review.class));
        verify(spamDetectionService).classifyForRegistration(content);
        verify(spamDetectionService, never()).detect(anyString());
    }

    private ReviewForm createForm(String content) {
        ReviewForm form = new ReviewForm();
        form.setRestaurantId(10L);
        form.setRating(5);
        form.setContent(content);
        return form;
    }
}
