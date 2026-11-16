package com.example.mymatzip.ai;

import com.example.mymatzip.service.ai.AiUsageLimiter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 1인 하루 호출 제한과 실패 시 횟수 되돌리기 검사
class AiUsageLimiterTest {

    @Test
    void 하루_한도를_넘으면_막고_실패한_호출은_되돌린다() {

        AiUsageLimiter limiter = new AiUsageLimiter(2);

        limiter.checkAndIncrease(1L);
        limiter.checkAndIncrease(1L);

        assertThat(limiter.remaining(1L)).isZero();
        assertThatThrownBy(() -> limiter.checkAndIncrease(1L)).isInstanceOf(IllegalStateException.class);

        limiter.refund(1L);
        limiter.refund(1L);

        assertThat(limiter.remaining(1L)).isEqualTo(1);
        // 다른 회원은 영향 없음
        assertThat(limiter.remaining(2L)).isEqualTo(2);
    }
}