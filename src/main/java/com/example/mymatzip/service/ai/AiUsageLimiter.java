package com.example.mymatzip.service.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// ============================================================
// [AiUsageLimiter] 회원 1명당 하루 AI 호출 횟수 제한 (C·D 공용)
// ============================================================
// 이유: OpenRouter 무료 모델은 계정(키)당 하루 50회까지만 호출된다.
//       한 사람이 계속 누르면 팀 전체가 AI 를 못 쓰게 되므로 1인 하루 횟수를 제한한다.
// 저장 위치: 서버 메모리(ConcurrentHashMap). 서버를 다시 켜면 횟수가 0 으로 돌아간다. (학습용으로 충분)
// ============================================================

// @Component: DefaultListableBeanFactory 객체가 이 클래스의 객체를 하나만 만들어 보관한다(싱글톤).
//   그래서 모든 요청이 같은 usage 맵을 함께 쓴다.
@Component
public class AiUsageLimiter {

    // 키: "회원번호:날짜" (예: "3:2026-10-28"), 값: 오늘 사용한 횟수
    // ConcurrentHashMap: 여러 요청 스레드가 동시에 숫자를 올려도 값이 꼬이지 않는 Map
    private final Map<String, Integer> usage = new ConcurrentHashMap<>();

    // application.properties 의 app.ai.daily-limit-per-member 값 (기본 5)
    private final int dailyLimit;

    // 생성자에 @Value 를 붙이면 DefaultListableBeanFactory 객체가 생성자를 호출할 때 설정 값을 넣어 준다.
    public AiUsageLimiter(@Value("${app.ai.daily-limit-per-member:5}") int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    // 호출하는 곳: AiReviewSummaryService.summarize(), AiRecommendService.recommend()
    // 반환: 없음. 횟수를 넘으면 IllegalStateException → AiApiController 가 429 응답으로 바꾼다.
    public void checkAndIncrease(Long memberId) {
        String key = memberId + ":" + LocalDate.now(ZoneId.of("Asia/Seoul"));
        // merge(): key 가 없으면 1 을 넣고, 있으면 기존 값 + 1 을 넣은 뒤 새 값을 돌려준다 (한 번에 처리)
        int count = usage.merge(key, 1, Integer::sum);
        if (count > dailyLimit) {
            throw new IllegalStateException("AI 기능은 하루 " + dailyLimit + "회까지 사용할 수 있습니다. 내일 다시 이용해 주세요.");
        }
    }

    // AI 호출이 실패했을 때 올렸던 횟수를 1 되돌린다 (실패한 호출은 횟수에서 빼 준다)
    // 호출하는 곳: AiReviewSummaryService, AiRecommendService 의 catch 블록
    public void refund(Long memberId) {
        String key = memberId + ":" + LocalDate.now(ZoneId.of("Asia/Seoul"));
        // computeIfPresent(): key 가 있을 때만 값을 바꾼다. 결과가 0 이하이면 null 을 돌려줘 key 를 지운다.
        usage.computeIfPresent(key, (k, v) -> v <= 1 ? null : v - 1);
    }

    // 오늘 남은 횟수 (화면 안내용)
    public int remaining(Long memberId) {
        String key = memberId + ":" + LocalDate.now(ZoneId.of("Asia/Seoul"));
        return Math.max(0, dailyLimit - usage.getOrDefault(key, 0));
    }
}