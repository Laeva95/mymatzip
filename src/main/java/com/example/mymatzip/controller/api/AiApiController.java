package com.example.mymatzip.controller.api;

import com.example.mymatzip.config.security.LoginMember;
import com.example.mymatzip.entity.Member;
import com.example.mymatzip.service.ai.AiRecommendService;
import com.example.mymatzip.service.ai.AiReviewSummaryService;
import com.example.mymatzip.service.ai.AiUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// ============================================================
// [AiApiController] AI 기능 API (C: 요약, D: 추천)
// ============================================================
// POST /api/ai/restaurants/{id}/summary  → AI 리뷰 요약   (detail.html 에서 fetch)
// POST /api/ai/recommend                 → AI 맛집 추천   (home.html 에서 fetch)
// 응답 코드: 200 성공 / 400 입력 오류 / 401 로그인 필요 / 429 하루 횟수 초과 / 503 AI 일시 오류
// ============================================================

// @RestController: 메서드의 반환 값을 화면 이름이 아니라 HTTP 응답 본문(JSON)으로 보낸다.
//   Map 을 JSON 으로 바꾸는 일은 MappingJackson2HttpMessageConverter 객체가 한다.
// @RequestMapping("/api/ai"): 이 클래스의 모든 주소 앞에 /api/ai 가 붙는다.
//   DispatcherServlet 객체가 요청 주소를 보고 RequestMappingHandlerMapping 객체에 등록된 메서드를 찾아 호출한다.
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiApiController {

    private final AiReviewSummaryService aiReviewSummaryService;
    private final AiRecommendService aiRecommendService;

    // ── AI 리뷰 요약 ─────────────────────────────────────────
    // 호출: detail.html 의 requestAiSummary() → fetch('/api/ai/restaurants/' + id + '/summary', {method:'POST'})
    // @PathVariable: 주소의 {id} 자리 값을 Long id 로 넣는다.
    // @LoginMember: LoginMemberArgumentResolver 객체가 SecurityContextHolder 에서 로그인 회원(Member)을 꺼내 넣는다.
    //   세션 로그인, JWT(Authorization: Bearer ...) 둘 다 같은 방식으로 들어온다. 로그인 안 했으면 null.
    @PostMapping("/restaurants/{id}/summary")
    public ResponseEntity<?> summary(@PathVariable Long id, @LoginMember Member loginMember) {
        if (loginMember == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인이 필요합니다."));
        }
        try {
            // 반환 Map { summary, cached, remaining } → JSON 으로 detail.html 에 전달
            return ResponseEntity.ok(aiReviewSummaryService.summarize(id, loginMember.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", e.getMessage()));
        } catch (AiUnavailableException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        }
    }

    // ── AI 맛집 추천 ─────────────────────────────────────────
    // 호출: home.html 의 추천 폼 → fetch('/api/ai/recommend', {method:'POST', body: JSON.stringify({question})})
    // @RequestBody: 요청 본문 JSON {"question":"..."} 을 MappingJackson2HttpMessageConverter 객체가 Map 으로 바꿔 넣는다.
    @PostMapping("/recommend")
    public ResponseEntity<?> recommend(@RequestBody Map<String, String> body, @LoginMember Member loginMember) {
        if (loginMember == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인이 필요합니다."));
        }
        try {
            // 반환 Map { picks:[{restaurant, reason}], remaining } → JSON 으로 home.html 에 전달
            return ResponseEntity.ok(aiRecommendService.recommend(body.get("question"), loginMember.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", e.getMessage()));
        } catch (AiUnavailableException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        }
    }
}