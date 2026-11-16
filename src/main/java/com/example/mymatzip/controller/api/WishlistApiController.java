/*
 * ============================================================================
 *  파일명    : WishlistApiController.java
 *  위치      : com.example.mymatzip.controller.api (API 컨트롤러 패키지)
 *  역할      : 찜(Wishlist) 관련 API 요청을 처리하는 "찜 목록 관리 창구" (REST API)
 * ============================================================================
 *
 *
 *  ■ MVC에서의 위치
 *    Controller 계층에 속합니다.
 *    [브라우저(JS)] → [이 Controller] → [WishlistService] → [WishlistRepository] → [DB]
 *
 *  ■ [Security] 변경 사항
 *    - 이전: HttpSession에서 (Member) session.getAttribute("loginMember")로 로그인 회원 조회
 *    - 현재: @LoginMember Member loginMember로 자동 주입 (Spring Security + ArgumentResolver)
 *
 *  ■ URL 매핑 요약
 *    기본 경로: /api/wishlists
 *    ┌──────────────────────────────────────┬────────┬────────────────────────┐
 *    │ URL                                  │ Method │ 설명                   │
 *    ├──────────────────────────────────────┼────────┼────────────────────────┤
 *    │ /api/wishlists                       │ GET    │ 내 찜 목록 조회        │
 *    │ /api/wishlists/{restaurantId}        │ POST   │ 맛집 찜하기            │
 *    │ /api/wishlists/{restaurantId}        │ DELETE │ 찜 취소                │
 *    │ /api/wishlists/{restaurantId}/check  │ GET    │ 찜 여부 확인           │
 *    └──────────────────────────────────────┴────────┴────────────────────────┘
 * ============================================================================
 */

// ── 패키지 선언 ──────────────────────────────────────────────
package com.example.mymatzip.controller.api;

// ── import 문 (외부 라이브러리 가져오기) ─────────────────────

// Member: 회원 엔티티 — [Security] @LoginMember로 자동 주입받아 사용
import com.example.mymatzip.entity.Member;

// [Security] @LoginMember: 현재 로그인한 회원 정보를 매개변수에 자동 주입하는 커스텀 어노테이션
import com.example.mymatzip.config.security.LoginMember;

// Wishlist: 찜 엔티티 — DB의 wishlist 테이블과 매핑되는 객체
// 어떤 회원이 어떤 맛집을 찜했는지의 정보를 담고 있음
import com.example.mymatzip.entity.Wishlist;

// WishlistService: 찜 관련 비즈니스 로직을 처리하는 서비스 클래스
import com.example.mymatzip.service.WishlistService;

// @RequiredArgsConstructor: final 필드의 생성자를 자동 생성하는 Lombok 어노테이션
import lombok.RequiredArgsConstructor;

// HttpStatus: HTTP 상태 코드 열거형 (200, 201, 400 등)
import org.springframework.http.HttpStatus;

// ResponseEntity: HTTP 응답 전체(상태코드 + 본문)를 담는 객체
import org.springframework.http.ResponseEntity;

// 스프링 웹 관련 어노테이션 모음
// @RestController, @RequestMapping, @GetMapping, @PostMapping,
// @DeleteMapping, @PathVariable 등 포함
import org.springframework.web.bind.annotation.*;

// LinkedHashMap: 입력 순서를 보장하는 Map (순서가 유지되는 키-값 쌍 자료구조)
// 일반 HashMap은 순서가 보장되지 않지만, LinkedHashMap은 넣은 순서대로 유지됩니다.
// JSON 응답에서 필드 순서를 예측 가능하게 만들기 위해 사용
import java.util.LinkedHashMap;

// List: 여러 개의 데이터를 순서대로 저장하는 자바 자료구조
import java.util.List;

// Map: 키-값 쌍으로 데이터를 저장하는 자바 자료구조
import java.util.Map;

// ── @RestController ─────────────────────────────────────────
// 이 클래스가 REST API 컨트롤러임을 선언 (JSON 데이터를 반환)
@RestController

// ── @RequestMapping("/api/wishlists") ───────────────────────
// 기본 URL 경로: /api/wishlists
// 이 클래스의 모든 메서드 URL 앞에 "/api/wishlists"가 자동으로 붙음
@RequestMapping("/api/wishlists")

// ── @RequiredArgsConstructor ────────────────────────────────
// final 필드(wishlistService)에 대한 생성자를 자동 생성 → 의존성 주입(DI)
@RequiredArgsConstructor
// ── 클래스 선언 ─────────────────────────────────────────────
public class WishlistApiController {

    // ── 필드 (멤버 변수) ────────────────────────────────────
    // WishlistService: 찜 관련 비즈니스 로직 담당 서비스
    private final WishlistService wishlistService;

    // ── GET /api/wishlists ──────────────────────────────────
    // 내 찜 목록 조회 API: 로그인한 회원이 찜한 맛집 목록을 반환
    // 요청 예시: GET http://localhost:8080/api/wishlists
    // 성공 응답: 200 OK +
    //   [{"id":1,"restaurant":{"id":5,"name":"맛있는집","category":"KOREAN",...},"createdAt":"..."},...]
    //
    // @GetMapping: GET 방식으로 "/api/wishlists" 요청이 오면 실행
    // 매개변수:
    //   - @LoginMember Member loginMember: [Security] 현재 로그인한 회원 (자동 주입)
    //     이전: HttpSession session → (Member) session.getAttribute("loginMember")
    //     현재: @LoginMember 어노테이션으로 깔끔하게 주입
    // 반환값: ResponseEntity<List<Map<String, Object>>>
    //   → 200 OK + 찜 목록 JSON 배열
    //   Map<String, Object>를 사용하는 이유: 찜 정보와 맛집 정보를 유연하게 구성하기 위해
    @GetMapping
    public ResponseEntity<?> myList(@LoginMember Member loginMember) {

        // ── 0단계: [Security] 로그인 확인 ─────────────────────
        // @LoginMember가 null을 반환하면 → 로그인하지 않은 상태
        // 401(Unauthorized) 응답을 반환하여 프론트엔드에서 처리하도록 함
        if (loginMember == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인이 필요합니다."));
        }

        // ── 1단계: 찜 목록 조회 및 응답 데이터 변환 ─────────
        // wishlistService.findByMemberId(loginMember.getId()):
        //   로그인한 회원의 ID로 찜 목록을 DB에서 조회
        //   반환값: List<Wishlist> (Wishlist 엔티티 목록)
        // .stream(): 목록을 스트림(흐름)으로 변환 (함수형 프로그래밍 방식)
        //   스트림 = 컨베이어 벨트처럼 데이터를 하나씩 처리하는 방식
        // .map(w -> { ... }): 각 Wishlist 엔티티를 Map(JSON 형태)으로 변환
        //   w = 각각의 Wishlist 객체 (반복 변수)
        // .toList(): 변환된 결과를 다시 List로 모음
        List<Map<String, Object>> result = wishlistService.findByMemberId(loginMember.getId())
                .stream()
                .map(w -> {
                    // ── 찜 정보를 담을 Map 생성 ────────────
                    // LinkedHashMap: 순서가 유지되는 Map
                    //   (id → restaurant → createdAt 순서로 JSON에 표시됨)
                    Map<String, Object> map = new LinkedHashMap<>();

                    // 찜 ID를 "id" 키로 저장
                    // w.getId(): 이 찜 데이터의 고유 번호
                    map.put("id", w.getId());

                    // ── 맛집 정보를 담을 중첩 Map 생성 ──────
                    // 맛집 정보를 별도의 Map으로 만들어서 JSON에서 중첩 구조로 표현
                    // 결과 예시: {"restaurant": {"id": 5, "name": "맛있는집", ...}}
                    Map<String, Object> rest = new LinkedHashMap<>();

                    // w.getRestaurant(): 이 찜에 연결된 맛집(Restaurant) 엔티티
                    // .getId(): 맛집 고유 번호
                    rest.put("id", w.getRestaurant().getId());

                    // .getName(): 맛집 이름
                    rest.put("name", w.getRestaurant().getName());

                    // .getCategory().name(): 카테고리의 영문 이름 (예: "KOREAN")
                    // .name()은 enum의 이름을 문자열로 반환하는 메서드
                    rest.put("category", w.getRestaurant().getCategory().name());

                    // .getCategory().getDisplayName(): 카테고리의 한글 표시명 (예: "한식")
                    rest.put("categoryDisplayName", w.getRestaurant().getCategory().getDisplayName());

                    // .getAddress(): 맛집 주소
                    rest.put("address", w.getRestaurant().getAddress());

                    // 맛집 정보 Map을 "restaurant" 키로 저장
                    map.put("restaurant", rest);

                    // w.getCreatedAt(): 찜한 날짜/시간
                    map.put("createdAt", w.getCreatedAt());

                    // 변환된 Map을 반환 (이것이 JSON의 한 항목이 됨)
                    return map;
                })
                .toList(); // 스트림 처리 결과를 List로 변환

        // ── 2단계: 200 OK + 찜 목록 JSON 반환 ───────────────
        return ResponseEntity.ok(result);
    }

    // ── POST /api/wishlists/{restaurantId} ──────────────────
    // 맛집 찜하기 API: 특정 맛집을 찜 목록에 추가
    // 요청 예시: POST http://localhost:8080/api/wishlists/5
    //   → 맛집 ID 5를 찜 목록에 추가
    // 성공 응답: 201 Created + {"message":"찜 완료!"}
    // 실패 응답: 400 Bad Request + {"error":"이미 찜한 맛집입니다."}
    //
    // @PostMapping("/{restaurantId}"):
    //   POST 방식으로 "/api/wishlists/숫자" 요청이 오면 실행
    // 매개변수:
    //   - @PathVariable Long restaurantId: 찜할 맛집의 ID (URL에서 추출)
    //   - @LoginMember Member loginMember: [Security] 현재 로그인한 회원 (자동 주입)
    // 반환값: ResponseEntity<?> (성공: 찜 완료 메시지, 실패: 에러 JSON)
    @PostMapping("/{restaurantId}")
    public ResponseEntity<?> add(@PathVariable Long restaurantId,
                                 @LoginMember Member loginMember) {
        // ── 0단계: [Security] 로그인 확인 ─────────────────────
        if (loginMember == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인이 필요합니다."));
        }
        try {
            // ── 1단계: 찜 추가 처리 ─────────────────────────
            // wishlistService.add(restaurantId, loginMember):
            //   해당 맛집을 로그인한 회원의 찜 목록에 추가
            //   이미 찜한 맛집이면 IllegalArgumentException 발생
            wishlistService.add(restaurantId, loginMember);

            // ── 2단계: 201 Created 성공 응답 ────────────────
            // HTTP 201(Created): "새로운 찜이 성공적으로 등록되었습니다"
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "찜 완료!"));
        } catch (IllegalArgumentException e) {
            // ── 3단계: 실패 시 에러 응답 ────────────────────
            // HTTP 400(Bad Request): "이미 찜한 맛집입니다" 등의 에러
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── DELETE /api/wishlists/{restaurantId} ─────────────────
    // 찜 취소 API: 찜한 맛집을 찜 목록에서 제거
    // 요청 예시: DELETE http://localhost:8080/api/wishlists/5
    //   → 맛집 ID 5를 찜 목록에서 제거
    // 성공 응답: 200 OK + {"message":"찜이 취소되었습니다."}
    // 실패 응답: 400 Bad Request + {"error":"찜하지 않은 맛집입니다."}
    //
    // @DeleteMapping("/{restaurantId}"):
    //   DELETE 방식으로 "/api/wishlists/숫자" 요청이 오면 실행
    // 매개변수:
    //   - @PathVariable Long restaurantId: 찜 취소할 맛집의 ID (URL에서 추출)
    //   - @LoginMember Member loginMember: [Security] 현재 로그인한 회원 (자동 주입)
    // 반환값: ResponseEntity<?> (성공: 취소 완료 메시지, 실패: 에러 JSON)
    @DeleteMapping("/{restaurantId}")
    public ResponseEntity<?> remove(@PathVariable Long restaurantId,
                                    @LoginMember Member loginMember) {
        // ── 0단계: [Security] 로그인 확인 ─────────────────────
        if (loginMember == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인이 필요합니다."));
        }
        try {
            // ── 1단계: 찜 취소 처리 ─────────────────────────
            // wishlistService.remove(restaurantId, loginMember):
            //   해당 맛집을 로그인한 회원의 찜 목록에서 제거
            //   찜하지 않은 맛집이면 IllegalArgumentException 발생
            wishlistService.remove(restaurantId, loginMember);

            // ── 2단계: 200 OK 성공 응답 ─────────────────────
            return ResponseEntity.ok(Map.of("message", "찜이 취소되었습니다."));
        } catch (IllegalArgumentException e) {
            // ── 3단계: 실패 시 에러 응답 ────────────────────
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/wishlists/{restaurantId}/check ─────────────
    // 찜 여부 확인 API: 로그인한 회원이 특정 맛집을 이미 찜했는지 확인
    // 요청 예시: GET http://localhost:8080/api/wishlists/5/check
    //   → 맛집 ID 5를 찜했는지 확인
    // 성공 응답: 200 OK + {"wished": true} 또는 {"wished": false}
    //
    // @GetMapping("/{restaurantId}/check"):
    //   GET 방식으로 "/api/wishlists/숫자/check" 요청이 오면 실행
    // 매개변수:
    //   - @PathVariable Long restaurantId: 확인할 맛집의 ID (URL에서 추출)
    //   - @LoginMember Member loginMember: [Security] 현재 로그인한 회원 (자동 주입)
    // 반환값: ResponseEntity<Map<String, Boolean>>
    //   → 200 OK + {"wished": true/false}
    @GetMapping("/{restaurantId}/check")
    public ResponseEntity<?> check(@PathVariable Long restaurantId,
                                   @LoginMember Member loginMember) {

        // ── 0단계: [Security] 로그인 확인 ─────────────────────
        if (loginMember == null) {
            return ResponseEntity.ok(Map.of("wished", false));
        }

        // ── 1단계: 찜 여부 확인 ─────────────────────────────
        // wishlistService.isWished(loginMember.getId(), restaurantId):
        //   로그인한 회원이 해당 맛집을 찜했는지 확인
        //   찜했으면 true, 안 했으면 false 반환
        boolean wished = wishlistService.isWished(loginMember.getId(), restaurantId);

        // ── 2단계: 200 OK + 찜 여부 JSON 반환 ───────────────
        // Map.of("wished", wished) → {"wished": true} 또는 {"wished": false}
        // 프론트엔드(JavaScript)에서 이 값으로 하트 아이콘의 상태를 결정합니다.
        //   wished가 true → 빨간 하트 (이미 찜한 상태)
        //   wished가 false → 빈 하트 (찜하지 않은 상태)
        return ResponseEntity.ok(Map.of("wished", wished));
    }
}