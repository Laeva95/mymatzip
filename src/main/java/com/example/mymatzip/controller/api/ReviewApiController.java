/*
 * ============================================================================
 *  파일명    : ReviewApiController.java
 *  위치      : com.example.mymatzip.controller.api (API 컨트롤러 패키지)
 *  역할      : 리뷰(Review) 관련 API 요청을 처리하는 "리뷰 관리 창구" (REST API)
 * ============================================================================
 *
 *
 *  ■ MVC에서의 위치
 *    Controller 계층에 속합니다.
 *    [브라우저(JS)] → [이 Controller] → [ReviewService] → [ReviewRepository] → [DB]
 *
 *  ■ [Security] 변경 사항
 *    - 이전: HttpSession에서 (Member) session.getAttribute("loginMember")로 로그인 회원 조회
 *    - 현재: @LoginMember Member loginMember로 자동 주입 (Spring Security + ArgumentResolver)
 *
 *  ■ URL 매핑 요약
 *    기본 경로: /api/reviews
 *    ┌────────────────────────────────────────┬────────┬──────────────────────┐
 *    │ URL                                    │ Method │ 설명                 │
 *    ├────────────────────────────────────────┼────────┼──────────────────────┤
 *    │ /api/reviews/restaurant/{restaurantId} │ GET    │ 특정 맛집의 리뷰 목록│
 *    │ /api/reviews                           │ POST   │ 새 리뷰 작성         │
 *    │ /api/reviews/{id}                      │ DELETE │ 리뷰 삭제            │
 *    └────────────────────────────────────────┴────────┴──────────────────────┘
 * ============================================================================
 */

// ── 패키지 선언 ──────────────────────────────────────────────
package com.example.mymatzip.controller.api;

// ── import 문 (외부 라이브러리 가져오기) ─────────────────────

// ReviewForm: 리뷰 작성 시 사용하는 데이터 전달 객체 (DTO)
// 맛집 ID, 별점(rating), 내용(content) 등의 필드를 가지고 있음
import com.example.mymatzip.dto.ReviewForm;

// ReviewResponse: 리뷰 조회 결과를 담는 응답용 DTO
// DB에서 가져온 리뷰 정보를 브라우저에 전달하기 좋은 형태로 변환한 것
import com.example.mymatzip.dto.ReviewResponse;

// Member: 회원 엔티티 — [Security] @LoginMember로 자동 주입받아 사용
import com.example.mymatzip.entity.Member;

// [Security] @LoginMember: 현재 로그인한 회원 정보를 매개변수에 자동 주입하는 커스텀 어노테이션
import com.example.mymatzip.config.security.LoginMember;

// ReviewService: 리뷰 관련 비즈니스 로직을 처리하는 서비스 클래스
import com.example.mymatzip.service.ReviewService;

// @Valid: 입력값 유효성 검증을 활성화하는 어노테이션
import jakarta.validation.Valid;

// @RequiredArgsConstructor: final 필드의 생성자를 자동 생성하는 Lombok 어노테이션
import lombok.RequiredArgsConstructor;

// HttpStatus: HTTP 상태 코드 열거형 (200, 201, 400, 403 등)
import org.springframework.http.HttpStatus;

// ResponseEntity: HTTP 응답 전체(상태코드 + 본문)를 담는 객체
import org.springframework.http.ResponseEntity;

// 스프링 웹 관련 어노테이션 모음
// @RestController, @RequestMapping, @GetMapping, @PostMapping,
// @DeleteMapping, @PathVariable, @RequestBody 등 포함
import org.springframework.web.bind.annotation.*;

// List: 여러 개의 데이터를 순서대로 저장하는 자바 자료구조
import java.util.List;

// Map: 키-값 쌍 자료구조 — JSON 응답 생성에 사용
import java.util.Map;

// ── @RestController ─────────────────────────────────────────
// 이 클래스가 REST API 컨트롤러임을 선언 (JSON 데이터를 반환)
@RestController

// ── @RequestMapping("/api/reviews") ─────────────────────────
// 기본 URL 경로: /api/reviews
// 이 클래스의 모든 메서드 URL 앞에 "/api/reviews"가 자동으로 붙음
@RequestMapping("/api/reviews")

// ── @RequiredArgsConstructor ────────────────────────────────
// final 필드(reviewService)에 대한 생성자를 자동 생성 → 의존성 주입(DI)
@RequiredArgsConstructor
// ── 클래스 선언 ─────────────────────────────────────────────
public class ReviewApiController {

    // ── 필드 (멤버 변수) ────────────────────────────────────
    // ReviewService: 리뷰 관련 비즈니스 로직 담당 서비스
    // 이 컨트롤러는 요청을 접수만 하고, 실제 처리는 Service에 위임합니다.
    private final ReviewService reviewService;

    // ── GET /api/reviews/restaurant/{restaurantId} ──────────
    // 특정 맛집의 리뷰 목록 조회 API
    // 요청 예시: GET http://localhost:8080/api/reviews/restaurant/1
    //   → 맛집 ID가 1인 맛집에 달린 모든 리뷰를 조회
    // 성공 응답: 200 OK + [{"id":1,"content":"맛있어요","rating":5,...}, {...}]
    //   (리뷰가 없으면 빈 배열 [] 반환)
    //
    // @GetMapping("/restaurant/{restaurantId}"):
    //   GET 방식으로 "/api/reviews/restaurant/숫자" 요청이 오면 실행
    // 매개변수:
    //   - @PathVariable Long restaurantId: URL에서 맛집 ID를 추출
    //     예: /api/reviews/restaurant/3 → restaurantId = 3
    // 반환값: ResponseEntity<List<ReviewResponse>>
    //   → 200 OK + 리뷰 목록 JSON 배열
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<ReviewResponse>> listByRestaurant(@PathVariable Long restaurantId) {
        // reviewService.findByRestaurantId(restaurantId):
        //   해당 맛집 ID에 속하는 모든 리뷰를 DB에서 조회하여 반환
        // ResponseEntity.ok(...): 200 OK + 리뷰 목록 JSON
        return ResponseEntity.ok(reviewService.findByRestaurantId(restaurantId));
    }

    // ── POST /api/reviews ───────────────────────────────────
    // 리뷰 작성 API: 맛집에 새로운 리뷰를 등록
    // 요청 예시: POST http://localhost:8080/api/reviews
    // 요청 Body: {"restaurantId":1,"rating":5,"content":"정말 맛있어요!"}
    // 성공 응답: 201 Created + {"id":1,"content":"정말 맛있어요!","rating":5,...}
    // 실패 응답: 400 Bad Request + {"error":"에러 메시지"}
    //
    // @PostMapping: POST 방식으로 "/api/reviews" 요청이 오면 실행
    // 매개변수:
    //   - @Valid @RequestBody ReviewForm form:
    //     요청 본문(JSON)을 ReviewForm 객체로 변환하고, 유효성 검증 수행
    //   - @LoginMember Member loginMember: [Security] 현재 로그인한 회원 (자동 주입)
    //     이전: HttpSession session → (Member) session.getAttribute("loginMember")
    //     현재: @LoginMember 어노테이션으로 깔끔하게 주입
    // 반환값: ResponseEntity<?> (성공: 리뷰 정보 JSON, 실패: 에러 JSON)
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ReviewForm form,
                                    @LoginMember Member loginMember) {
        try {
            // ── 1단계: 리뷰 저장 처리 ───────────────────────
            // reviewService.save(form, loginMember):
            //   리뷰 데이터(form)와 작성자(loginMember)를 Service에 전달하여 DB에 저장
            //   해당 맛집이 존재하지 않으면 IllegalArgumentException 발생
            ReviewResponse response = reviewService.save(form, loginMember);

            // ── 2단계: 201 Created 성공 응답 ────────────────
            // HTTP 201(Created): "새 리소스(리뷰)가 성공적으로 생성되었습니다"
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            // ── 3단계: 실패 시 에러 응답 ────────────────────
            // HTTP 400(Bad Request): "요청이 잘못되었습니다"
            // 예: 존재하지 않는 맛집에 리뷰를 달려고 한 경우
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── DELETE /api/reviews/{id} ────────────────────────────
    // 리뷰 삭제 API: 자신이 작성한 리뷰를 삭제
    // 요청 예시: DELETE http://localhost:8080/api/reviews/1
    // 성공 응답: 200 OK + {"message":"리뷰가 삭제되었습니다."}
    // 실패 응답: 403 Forbidden + {"error":"삭제 권한이 없습니다."}
    //
    // @DeleteMapping("/{id}"): DELETE 방식으로 "/api/reviews/숫자" 요청이 오면 실행
    // 매개변수:
    //   - @PathVariable Long id: 삭제할 리뷰의 ID (URL에서 추출)
    //   - @LoginMember Member loginMember: [Security] 현재 로그인한 회원 (자동 주입)
    // 반환값: ResponseEntity<?> (성공: 삭제 완료 메시지, 실패: 에러 JSON)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    @LoginMember Member loginMember) {
        try {
            // ── 1단계: 리뷰 삭제 처리 ───────────────────────
            // reviewService.delete(id, loginMember):
            //   삭제할 리뷰 ID와 요청자(loginMember)를 전달
            //   Service에서 본인이 작성한 리뷰인지 확인 후 삭제
            //   본인이 아니면 IllegalArgumentException 발생
            reviewService.delete(id, loginMember);

            // ── 2단계: 삭제 성공 응답 ───────────────────────
            // 200 OK + {"message": "리뷰가 삭제되었습니다."} JSON 반환
            return ResponseEntity.ok(Map.of("message", "리뷰가 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            // ── 3단계: 삭제 실패 시 에러 응답 ───────────────
            // HTTP 403(Forbidden): "권한이 없습니다"
            // 예: 다른 사람이 작성한 리뷰를 삭제하려는 경우
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    // ── POST /api/reviews/{id}/report ─────────────────────
    // 리뷰 신고 API: 부적절한 리뷰를 관리자에게 신고
    @PostMapping("/{id}/report")
    public ResponseEntity<?> report(@PathVariable Long id,
                                    @LoginMember Member loginMember) {
        try {
            reviewService.report(id);
            return ResponseEntity.ok(Map.of("message", "리뷰가 신고되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}