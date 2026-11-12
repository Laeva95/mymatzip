package com.example.mymatzip.controller.api.publicapi;

import com.example.mymatzip.config.security.LoginMember;
import com.example.mymatzip.dto.publicapi.ImportReport;
import com.example.mymatzip.entity.Member;
import com.example.mymatzip.entity.Source;
import com.example.mymatzip.service.publicapi.PublicRestaurantImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// ============================================================
// [AdminRestaurantImportController] 공공 API 맛집 수집 — 관리자 전용
// ============================================================
// 권한: SecurityConfig의 .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
//       → 이 컨트롤러는 따로 권한 코드를 넣지 않아도 ADMIN만 들어옵니다.
//       /api/** 는 CSRF 제외 → Postman에서 JWT(Authorization: Bearer)로 호출 가능
//
// 주소 (모두 POST, limit은 선택 — 처음엔 limit=10으로 테스트)
//   /api/admin/restaurants/register/publicapi/model/preview?limit=10   모범음식점 조사 (저장 안 함)
//   /api/admin/restaurants/register/publicapi/model?limit=10           모범음식점 저장
//   /api/admin/restaurants/register/publicapi/food/preview?limit=10    부산맛집 조사 (저장 안 함)
//   /api/admin/restaurants/register/publicapi/food?limit=10            부산맛집 저장
//   limit을 빼면 전체 (부산맛집 약 536건, 모범음식점 약 268건)
//
// 응답: ImportReport JSON (요약 숫자, 중복·제외·실패·매핑없음 목록, 분류별 건수, CSV 파일 경로)
// 오류: 키 없음 · API 오류 → 503 {"error": ...} / 잘못된 요청 → 400 {"error": ...}
// ============================================================
@RestController
@RequestMapping("/api/admin/restaurants/register/publicapi")
@RequiredArgsConstructor
public class AdminRestaurantImportController {

    private final PublicRestaurantImportService importService;

    // ── 모범음식점 조사 ─────────────────────────────────────────
    //
    @PostMapping("/model/preview")
    public ResponseEntity<?> previewModel(@RequestParam(defaultValue = "0") int limit) {
        try {
            ImportReport report = importService.preview(Source.PUBLIC_API_BUSAN_MODEL, limit);
            return ResponseEntity.ok(report);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        }
    }

    // ── 모범음식점 저장 ─────────────────────────────────────────
    @PostMapping("/model")
    public ResponseEntity<?> importModel(@RequestParam(defaultValue = "0") int limit,
                                         @LoginMember Member loginMember) {
        try {
            ImportReport report = importService.importAll(Source.PUBLIC_API_BUSAN_MODEL, limit, loginMember);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        }
    }

    // ── 부산맛집 조사 ───────────────────────────────────────────
    //남은 매핑없음 3건(태백관, 더스타, 갤러리움웨딩홀부페)은 업태도 없고 키워드도 없어서 기타로 저장됩니다. 정상입니다.
    @PostMapping("/food/preview")
    public ResponseEntity<?> previewFood(@RequestParam(defaultValue = "0") int limit) {
        try {
            ImportReport report = importService.preview(Source.PUBLIC_API_BUSAN_FOOD, limit);
            return ResponseEntity.ok(report);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        }
    }

    // ── 부산맛집 저장 ───────────────────────────────────────────
    @PostMapping("/food")
    public ResponseEntity<?> importFood(@RequestParam(defaultValue = "0") int limit,
                                        @LoginMember Member loginMember) {
        try {
            ImportReport report = importService.importAll(Source.PUBLIC_API_BUSAN_FOOD, limit, loginMember);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        }
    }
}
