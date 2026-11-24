// [C수정] 오탐 방지 스팸 규칙 테스트 보강
package com.example.mymatzip.ai;

import com.example.mymatzip.service.ai.AiReviewSpamDetectionService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiReviewSpamDetectionServiceTest {
    @Test
    void spam응답은_스팸으로_파싱된다() {
        AiReviewSpamDetectionService.Result result = AiReviewSpamDetectionService.parse(
                "SPAM|외부 링크를 반복적으로 홍보합니다.\n추가 설명"
        );

        assertThat(result.spam()).isTrue();
        assertThat(result.reason()).isEqualTo("외부 링크를 반복적으로 홍보합니다.");
    }

    @Test
    void 정상응답은_정상으로_파싱된다() {
        AiReviewSpamDetectionService.Result result = AiReviewSpamDetectionService.parse(
                "NORMAL|음식의 맛과 양에 대한 일반적인 방문 후기입니다."
        );

        assertThat(result.spam()).isFalse();
        assertThat(result.reason()).contains("일반적인 방문 후기");
    }

    @Test
    void 응답이_비어있으면_정상으로_처리한다() {
        assertThat(AiReviewSpamDetectionService.parse(null).spam()).isFalse();
        assertThat(AiReviewSpamDetectionService.parse(" ").spam()).isFalse();
    }

    @Test
    void BLOCK_응답은_등록차단으로_파싱된다() {
        AiReviewSpamDetectionService.Result result = AiReviewSpamDetectionService.parse(
                "BLOCK|외부 사이트 가입을 유도하는 명백한 광고입니다."
        );

        assertThat(result.spam()).isTrue();
        assertThat(result.blocked()).isTrue();
    }

    @Test
    void 명백한_불법광고만_차단하고_일반적인_링크광고는_의심으로_낮춘다() {
        assertThat(AiReviewSpamDetectionService.isDefinitelySpam("코인 투자로 고수익 보장"))
                .isFalse();
        assertThat(AiReviewSpamDetectionService.isDefinitelySpam("코인 투자 고수익 보장 상담 및 신청은 링크로"))
                .isTrue();
        assertThat(AiReviewSpamDetectionService.isDefinitelySpam("무료 쿠폰은 example.com에서 신청하세요"))
                .isFalse();
        assertThat(AiReviewSpamDetectionService.isSuspiciousSpam("무료 쿠폰은 example.com에서 신청하세요"))
                .isTrue();
        assertThat(AiReviewSpamDetectionService.isDefinitelySpam("불법주차가 많아요"))
                .isFalse();
        assertThat(AiReviewSpamDetectionService.isDefinitelySpam("가게가 대출받아 리모델링했대요"))
                .isFalse();
        assertThat(AiReviewSpamDetectionService.isDefinitelySpam("김치찌개가 맛있고 양도 많아요"))
                .isFalse();
    }
}
