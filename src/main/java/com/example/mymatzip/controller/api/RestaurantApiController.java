/*
 * ============================================================================
 *  파일명    : RestaurantApiController.java
 *  위치      : com.example.mymatzip.controller.api (API 컨트롤러 패키지)
 *  역할      : 맛집(Restaurant) 관련 API 요청을 처리하는 "맛집 관리 창구" (REST API)
 * ============================================================================
 *
 *
 *  ■ MVC에서의 위치
 *    Controller 계층에 속합니다.
 *    [브라우저(JS)] → [이 Controller] → [RestaurantService] → [RestaurantRepository] → [DB]
 *
 *  ■ RESTful API 설계
 *    REST 원칙에 따라 HTTP 메서드로 동작을 구분합니다:
 *    - GET    = 조회 (읽기)
 *    - POST   = 생성 (새로 만들기)
 *    - PUT    = 수정 (전체 업데이트)
 *    - DELETE = 삭제
 *
 *  ■ [Security] 변경 사항
 *    - 이전: HttpSession에서 (Member) session.getAttribute("loginMember")로 로그인 회원 조회
 *    - 현재: @LoginMember Member loginMember로 자동 주입 (Spring Security + ArgumentResolver)
 *
 *  ■ URL 매핑 요약
 *    기본 경로: /api/restaurants
 *    ┌──────────────────────────────┬────────┬──────────────────────────────┐
 *    │ URL                          │ Method │ 설명                         │
 *    ├──────────────────────────────┼────────┼──────────────────────────────┤
 *    │ /api/restaurants             │ GET    │ 전체 목록 (카테고리 필터 가능)│
 *    │ /api/restaurants/{id}        │ GET    │ 특정 맛집 상세 조회          │
 *    │ /api/restaurants             │ POST   │ 새 맛집 등록                 │
 *    │ /api/restaurants/{id}        │ PUT    │ 맛집 정보 수정               │
 *    │ /api/restaurants/{id}        │ DELETE │ 맛집 삭제                    │
 *    │ /api/restaurants/search      │ GET    │ 키워드로 맛집 검색           │
 *    └──────────────────────────────┴────────┴──────────────────────────────┘
 * ============================================================================
 */

// ── 패키지 선언 ──────────────────────────────────────────────
package com.example.mymatzip.controller.api;

// ── import 문 (외부 라이브러리 가져오기) ─────────────────────

// RestaurantForm: 맛집 등록/수정 시 사용하는 데이터 전달 객체 (DTO)
// 맛집 이름, 카테고리, 주소, 설명 등의 필드를 가지고 있음
import com.example.mymatzip.dto.RestaurantForm;

// RestaurantResponse: 맛집 조회 결과를 담는 응답용 DTO
// DB에서 가져온 맛집 정보를 브라우저에 전달하기 좋은 형태로 변환한 것
import com.example.mymatzip.dto.RestaurantResponse;

// Category: 맛집 카테고리를 정의하는 열거형(enum)
// 예: KOREAN(한식), JAPANESE(일식), CHINESE(중식) 등
import com.example.mymatzip.entity.Category;

// Member: 회원 정보를 담는 엔티티(Entity) 클래스
// [Security] @LoginMember로 자동 주입받아 사용
import com.example.mymatzip.entity.Member;

// [Security] @LoginMember: 현재 로그인한 회원 정보를 매개변수에 자동 주입하는 커스텀 어노테이션
import com.example.mymatzip.config.security.LoginMember;

// RestaurantService: 맛집 관련 비즈니스 로직을 처리하는 서비스 클래스
import com.example.mymatzip.service.RestaurantService;

// @Valid: 입력값 유효성 검증 활성화 어노테이션
import jakarta.validation.Valid;

// @RequiredArgsConstructor: final 필드의 생성자를 자동 생성하는 Lombok 어노테이션
import lombok.RequiredArgsConstructor;

// HttpStatus: HTTP 상태 코드 열거형 (200, 201, 400, 403, 404 등)
import org.springframework.http.HttpStatus;

// ResponseEntity: HTTP 응답 전체(상태코드 + 본문)를 담는 객체
import org.springframework.http.ResponseEntity;

// @RestController, @RequestMapping 등: 스프링 웹 관련 어노테이션 모음
// *로 가져오면 해당 패키지의 모든 클래스를 한번에 import합니다.
// @GetMapping, @PostMapping, @PutMapping, @DeleteMapping,
// @PathVariable, @RequestParam, @RequestBody 등이 포함됩니다.
import org.springframework.web.bind.annotation.*;

// List: 여러 개의 데이터를 순서대로 저장하는 자바 자료구조
// 예: 맛집 목록을 List<RestaurantResponse>로 반환
import java.util.List;

// Map: 키-값 쌍으로 데이터를 저장하는 자바 자료구조
// JSON 응답 생성에 사용
import java.util.Map;

// ── @RestController ─────────────────────────────────────────
// 이 클래스가 REST API 컨트롤러임을 선언 (JSON 데이터 반환)
@RestController

// ── @RequestMapping("/api/restaurants") ─────────────────────
// 기본 URL 경로: /api/restaurants
// 이 클래스의 모든 메서드 URL 앞에 "/api/restaurants"가 자동으로 붙음
@RequestMapping("/api/restaurants")

// ── @RequiredArgsConstructor ────────────────────────────────
// final 필드(restaurantService)에 대한 생성자를 자동 생성 → 의존성 주입(DI)
@RequiredArgsConstructor
// ── 클래스 선언 ─────────────────────────────────────────────
public class RestaurantApiController {

    // ── 필드 (멤버 변수) ────────────────────────────────────
    // RestaurantService: 맛집 관련 비즈니스 로직 담당 서비스
    // 이 컨트롤러는 요청만 받고, 실제 처리는 Service에 위임합니다.
    private final RestaurantService restaurantService;

    // ── GET /api/restaurants ────────────────────────────────
    // 맛집 목록 조회 API: 전체 맛집 또는 카테고리별 맛집 목록을 반환
    // 요청 예시 1: GET http://localhost:8080/api/restaurants (전체 목록)
    // 요청 예시 2: GET http://localhost:8080/api/restaurants?category=KOREAN (한식만)
    // 성공 응답: 200 OK + [{"id":1,"name":"맛있는집",...}, {...}]
    //
    // @GetMapping: GET 방식으로 "/api/restaurants" 요청이 오면 실행
    //   (경로를 지정하지 않으면 클래스의 @RequestMapping 경로 그대로 사용)
    // 매개변수:
    //   - @RequestParam(required = false) Category category:
    //     URL의 쿼리 파라미터에서 "category" 값을 가져옴
    //     required = false: 이 값은 필수가 아님 (없으면 null)
    //     예: ?category=KOREAN → category = Category.KOREAN
    //     예: (파라미터 없음) → category = null
    // 반환값: ResponseEntity<List<RestaurantResponse>>
    //   → HTTP 200 + 맛집 목록 JSON 배열
    @GetMapping
    public ResponseEntity<List<RestaurantResponse>> list(
            @RequestParam(required = false) Category category) {

        // ── 1단계: 카테고리 필터링 여부 확인 ────────────────
        // 삼항 연산자: (조건) ? (참일 때 값) : (거짓일 때 값)
        // category != null → 카테고리가 지정됨 → 해당 카테고리의 맛집만 조회
        // category == null → 카테고리 미지정 → 전체 맛집 조회
        List<RestaurantResponse> restaurants = (category != null)
                ? restaurantService.findByCategory(category)  // 카테고리별 조회
                : restaurantService.findAll();                 // 전체 조회

        // ── 2단계: 200 OK + 맛집 목록 JSON 반환 ─────────────
        // ResponseEntity.ok(restaurants):
        //   HTTP 200(OK) 상태코드와 함께 맛집 목록을 JSON으로 반환
        return ResponseEntity.ok(restaurants);
    }

    // ── GET /api/restaurants/{id} ───────────────────────────
    // 맛집 상세 조회 API: 특정 맛집의 상세 정보를 반환
    // 요청 예시: GET http://localhost:8080/api/restaurants/1
    // 성공 응답: 200 OK + {"id":1,"name":"맛있는집","category":"KOREAN",...}
    // 실패 응답: 404 Not Found + {"error":"해당 맛집을 찾을 수 없습니다."}
    //
    // @GetMapping("/{id}"): GET 방식으로 "/api/restaurants/숫자" 요청이 오면 실행
    //   {id} = 경로 변수(PathVariable) — URL에서 값을 추출
    // 매개변수:
    //   - @PathVariable Long id: URL의 {id} 부분을 Long 타입 변수로 받음
    //     예: /api/restaurants/3 → id = 3
    // 반환값: ResponseEntity<?> (성공: 맛집 정보 JSON, 실패: 에러 JSON)
    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable Long id) {
        try {
            // ── 1단계: ID로 맛집 조회 ───────────────────────
            // restaurantService.findById(id): 해당 ID의 맛집을 DB에서 조회
            // 맛집이 없으면 IllegalArgumentException 발생 → catch 블록으로 이동
            // ResponseEntity.ok(...): 200 OK + 맛집 상세 정보 JSON
            return ResponseEntity.ok(restaurantService.findById(id));
        } catch (IllegalArgumentException e) {
            // ── 2단계: 맛집을 찾지 못한 경우 ────────────────
            // HTTP 404(Not Found): "요청한 리소스를 찾을 수 없습니다"
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // ── POST /api/restaurants ────────────────────────────────
    // 맛집 등록 API: 새로운 맛집 정보를 DB에 저장
    // 요청 예시: POST http://localhost:8080/api/restaurants
    // 요청 Body: {"name":"맛있는집","category":"KOREAN","address":"서울시...","description":"..."}
    // 성공 응답: 201 Created + {"id":1,"name":"맛있는집",...}
    // 실패 응답: 400 Bad Request + {"error":"에러 메시지"}
    //
    // @PostMapping: POST 방식으로 "/api/restaurants" 요청이 오면 실행
    // 매개변수:
    //   - @Valid @RequestBody RestaurantForm form: 유효성 검증된 맛집 등록 데이터
    //   - @LoginMember Member loginMember: [Security] 현재 로그인한 회원 정보 (자동 주입)
    //     이전: HttpSession session → (Member) session.getAttribute("loginMember")
    //     현재: @LoginMember 어노테이션으로 깔끔하게 주입
    // 반환값: ResponseEntity<?> (성공: 맛집 정보 JSON, 실패: 에러 JSON)
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody RestaurantForm form,
                                    @LoginMember Member loginMember) {
        try {
            // ── 1단계: 맛집 등록 처리 ───────────────────────
            // restaurantService.save(form, loginMember):
            //   맛집 정보(form)와 등록자(loginMember)를 Service에 전달하여 DB에 저장
            // 반환값: 저장된 맛집의 응답 DTO (RestaurantResponse)
            RestaurantResponse response = restaurantService.save(form, loginMember);

            // ── 2단계: 201 Created 응답 반환 ────────────────
            // HTTP 201(Created): "새로운 리소스가 성공적으로 생성되었습니다"
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            // ── 3단계: 등록 실패 시 에러 응답 ───────────────
            // HTTP 400(Bad Request): "요청이 잘못되었습니다"
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── PUT /api/restaurants/{id} ───────────────────────────
    // 맛집 수정 API: 기존 맛집 정보를 수정 (본인이 등록한 맛집만 가능)
    // 요청 예시: PUT http://localhost:8080/api/restaurants/1
    // 요청 Body: {"name":"수정된 맛집","category":"JAPANESE","address":"...","description":"..."}
    // 성공 응답: 200 OK + {"id":1,"name":"수정된 맛집",...}
    // 실패 응답: 403 Forbidden + {"error":"수정 권한이 없습니다."}
    //
    // @PutMapping("/{id}"): PUT 방식으로 "/api/restaurants/숫자" 요청이 오면 실행
    //   PUT = 리소스 전체 수정 (REST 규약)
    // 매개변수:
    //   - @PathVariable Long id: 수정할 맛집의 ID (URL에서 추출)
    //   - @Valid @RequestBody RestaurantForm form: 수정할 맛집 데이터
    //   - @LoginMember Member loginMember: [Security] 현재 로그인한 회원 (자동 주입)
    // 반환값: ResponseEntity<?> (성공: 수정된 맛집 JSON, 실패: 에러 JSON)
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @Valid @RequestBody RestaurantForm form,
                                    @LoginMember Member loginMember) {
        try {
            // ── 1단계: 맛집 수정 처리 ───────────────────────
            // restaurantService.update(id, form, loginMember):
            //   수정할 맛집 ID, 새로운 데이터, 요청한 회원 정보를 전달
            //   Service에서 본인이 등록한 맛집인지 확인 후 수정
            //   권한이 없으면 IllegalArgumentException 발생
            // ResponseEntity.ok(...): 200 OK + 수정된 맛집 정보 JSON
            return ResponseEntity.ok(restaurantService.update(id, form, loginMember));
        } catch (IllegalArgumentException e) {
            // ── 2단계: 수정 실패 시 에러 응답 ───────────────
            // HTTP 403(Forbidden): "접근 권한이 없습니다" (본인이 등록한 맛집이 아닌 경우)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    // ── DELETE /api/restaurants/{id} ────────────────────────
    // 맛집 삭제 API: 맛집을 DB에서 삭제 (본인이 등록한 맛집만 가능)
    // 요청 예시: DELETE http://localhost:8080/api/restaurants/1
    // 성공 응답: 200 OK + {"message":"삭제되었습니다."}
    // 실패 응답: 403 Forbidden + {"error":"삭제 권한이 없습니다."}
    //
    // @DeleteMapping("/{id}"): DELETE 방식으로 "/api/restaurants/숫자" 요청이 오면 실행
    //   DELETE = 리소스 삭제 (REST 규약)
    // 매개변수:
    //   - @PathVariable Long id: 삭제할 맛집의 ID (URL에서 추출)
    //   - @LoginMember Member loginMember: [Security] 현재 로그인한 회원 (자동 주입)
    // 반환값: ResponseEntity<?> (성공: 삭제 완료 메시지, 실패: 에러 JSON)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    @LoginMember Member loginMember) {
        try {
            // ── 1단계: 맛집 삭제 처리 ───────────────────────
            // restaurantService.delete(id, loginMember):
            //   삭제할 맛집 ID와 요청한 회원 정보를 전달
            //   Service에서 본인이 등록한 맛집인지 확인 후 삭제
            //   권한이 없으면 IllegalArgumentException 발생
            restaurantService.delete(id, loginMember);

            // ── 2단계: 삭제 성공 응답 ───────────────────────
            // 200 OK + {"message": "삭제되었습니다."} JSON 반환
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            // ── 3단계: 삭제 실패 시 에러 응답 ───────────────
            // HTTP 403(Forbidden): "삭제 권한이 없습니다"
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/restaurants/search ─────────────────────────
    // 맛집 검색 API: 키워드로 맛집을 검색하여 결과 목록을 반환
    // 요청 예시: GET http://localhost:8080/api/restaurants/search?keyword=치킨
    // 성공 응답: 200 OK + [{"id":1,"name":"맛있는 치킨집",...}, {...}]
    //   (검색 결과가 없으면 빈 배열 [] 반환)
    //
    // @GetMapping("/search"): GET 방식으로 "/api/restaurants/search" 요청이 오면 실행
    // 매개변수:
    //   - @RequestParam String keyword: URL 쿼리 파라미터에서 검색어를 가져옴
    //     예: ?keyword=치킨 → keyword = "치킨"
    //     required의 기본값은 true이므로, keyword 파라미터가 반드시 있어야 함
    // 반환값: ResponseEntity<List<RestaurantResponse>>
    //   → 200 OK + 검색 결과 맛집 목록 JSON 배열
    @GetMapping("/search")
    public ResponseEntity<List<RestaurantResponse>> search(@RequestParam String keyword) {
        // restaurantService.search(keyword): 키워드로 맛집 검색
        //   맛집 이름이나 주소에 키워드가 포함된 맛집들을 찾아서 반환
        // ResponseEntity.ok(...): 200 OK + 검색 결과 JSON 반환
        return ResponseEntity.ok(restaurantService.search(keyword));
    }
}
