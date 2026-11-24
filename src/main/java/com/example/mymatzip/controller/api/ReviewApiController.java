// [C수정] 리뷰 페이징 제한 및 로그인 회원 상태 반영
/*
 * ============================================================================
 *  파일명    : ReviewApiController.java
 *  위치      : com.example.mymatzip.controller.api (API 컨트롤러 패키지)
 *  역할      : 리뷰(Review) 관련 API 요청을 처리하는 "리뷰 관리 창구" (REST API)
 * ============================================================================
 *
 *  ■ MVC에서의 위치
 *    Controller 계층에 속합니다.
 *    [브라우저(JS)] → [이 Controller] → [ReviewService] → [ReviewRepository] → [DB]
 *
 *  ■ URL 매핑 요약
 *    기본 경로: /api/reviews
 *    ┌────────────────────────────────────────┬────────┬──────────────────────┐
 *    │ URL                                    │ Method │ 설명                 │
 *    ├────────────────────────────────────────┼────────┼──────────────────────┤
 *    │ /api/reviews/restaurant/{restaurantId} │ GET    │ 특정 맛집의 리뷰 목록│
 *    │ /api/reviews                           │ POST   │ 새 리뷰 작성         │
 *    │ /api/reviews/{id}                      │ DELETE │ 리뷰 삭제            │
 *    │ /api/reviews/{id}/report               │ POST   │ 리뷰 신고            │
 *    └────────────────────────────────────────┴────────┴──────────────────────┘
 * ============================================================================
 */

package com.example.mymatzip.controller.api;

import com.example.mymatzip.config.security.LoginMember;
import com.example.mymatzip.dto.PageResponse;
import com.example.mymatzip.dto.ReviewForm;
import com.example.mymatzip.dto.ReviewResponse;
import com.example.mymatzip.entity.Member;
import com.example.mymatzip.service.ReviewService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewApiController {

    private final ReviewService reviewService;

    // ── 1. GET /api/reviews/restaurant/{restaurantId} ───────
    // 특정 맛집의 리뷰 목록 조회 API
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<PageResponse<ReviewResponse>> listByRestaurant(
            @PathVariable Long restaurantId,
            @LoginMember Member loginMember,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return ResponseEntity.ok(
                reviewService.findByRestaurantId(restaurantId, pageable, loginMember)
        );
    }

    // ── 2. POST /api/reviews ────────────────────────────────
    // 리뷰 작성 API: 맛집에 새로운 리뷰를 등록
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ReviewForm form,
                                    @LoginMember Member loginMember) {
        // 1단계: 비로그인 사용자 방어 (401 Unauthorized)
        if (loginMember == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인이 필요한 서비스입니다."));
        }

        try {
            // 2단계: 리뷰 저장 처리 및 201 Created 반환
            ReviewResponse response = reviewService.save(form, loginMember);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

            // [추가] 본인 가게 등 권한 없음 (403 Forbidden)
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── 3. DELETE /api/reviews/{id} ─────────────────────────
    // 리뷰 삭제 API: 자신이 작성한 리뷰를 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id,
                                                      @LoginMember Member loginMember) {
        // 1단계: 비로그인 사용자 방어 (401 Unauthorized)
        if (loginMember == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인이 필요한 서비스입니다."));
        }

        try {
            // 2단계: 리뷰 삭제 처리 및 200 OK 반환
            reviewService.delete(id, loginMember);
            return ResponseEntity.ok(Map.of("message", "리뷰가 삭제되었습니다."));

        } catch (EntityNotFoundException e) {
            // 3단계-A: 리뷰가 존재하지 않을 때 (404 Not Found)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));

        } catch (AccessDeniedException e) {
            // 3단계-B: 작성자 본인이 아닐 때 (403 Forbidden)
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // [추가] 리뷰 도움돼요 등록
    @PostMapping("/{id}/helpful")
    public ResponseEntity<Map<String, String>> addHelpful(
            @PathVariable Long id,
            @LoginMember Member loginMember) {

        if (loginMember == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인이 필요한 서비스입니다."));
        }

        try {
            reviewService.addHelpful(id, loginMember);
            return ResponseEntity.ok(Map.of("message", "도움돼요를 등록했습니다."));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 회원이 중복 요청한 경우 DB 유니크 제약조건으로 차단
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "이미 도움돼요를 눌렀습니다."));
        }
    }

    // [추가] 리뷰 도움돼요 취소
    @DeleteMapping("/{id}/helpful")
    public ResponseEntity<Map<String, String>> removeHelpful(
            @PathVariable Long id,
            @LoginMember Member loginMember) {

        if (loginMember == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인이 필요한 서비스입니다."));
        }

        try {
            reviewService.removeHelpful(id, loginMember);
            return ResponseEntity.ok(Map.of("message", "도움돼요를 취소했습니다."));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // [추가] 맛집별 인기 키워드 조회
    @GetMapping("/restaurant/{restaurantId}/keywords")
    public ResponseEntity<?> popularKeywords(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(reviewService.getPopularKeywords(restaurantId));
    }

    // ── 4. POST /api/reviews/{id}/report ────────────────────
// 리뷰 신고 API: 부적절한 리뷰를 관리자에게 신고
    @PostMapping("/{id}/report")
    public ResponseEntity<Map<String, String>> report(
            @PathVariable Long id,
            @LoginMember Member loginMember,
            @RequestBody(required = false) Map<String, String> body) {

        // 1단계: 비로그인 사용자 방어 (401 Unauthorized)
        if (loginMember == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인이 필요한 서비스입니다."));
        }

        try {
            // 2단계: 리뷰 신고 처리 및 200 OK 반환
            String reason = (body == null) ? null : body.get("reason");
            reviewService.report(id, loginMember, reason);
            return ResponseEntity.ok(
                    Map.of("message", "리뷰가 신고되었습니다.")
            );

        } catch (EntityNotFoundException e) {
            // 3단계-A: 신고 대상 리뷰가 없을 때 (404 Not Found)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));

        } catch (IllegalArgumentException e) {
            // 3단계-B: 본인 리뷰 신고 또는 이미 신고된 리뷰 (400 Bad Request)
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }

    }
}
