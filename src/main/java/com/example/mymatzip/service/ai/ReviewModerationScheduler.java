package com.example.mymatzip.service.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 주기적으로 배치 실행 조건(10건 이상 또는 오래된 대기 건)을 확인한다. */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "app.ai.review-moderation.enabled", havingValue = "true", matchIfMissing = true)
public class ReviewModerationScheduler {

    private final ReviewModerationBatchService batchService;

    public ReviewModerationScheduler(ReviewModerationBatchService batchService) {
        this.batchService = batchService;
    }

    @Scheduled(fixedDelayString = "${app.ai.review-moderation.check-interval-ms:60000}")
    public void run() {
        batchService.runIfNeeded();
    }
}