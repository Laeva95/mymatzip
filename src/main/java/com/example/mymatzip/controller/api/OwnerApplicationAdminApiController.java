package com.example.mymatzip.controller.api;

import com.example.mymatzip.dto.OwnerApplicationResponse;
import com.example.mymatzip.entity.OwnerApplication;
import com.example.mymatzip.entity.OwnerApplicationStatus;
import com.example.mymatzip.service.OwnerApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

// ============================================================
// [OwnerApplicationAdminApiController.java] 식당 사장 신청 심사 API (관리자용)
// ============================================================
//
// GET  /api/admin/owner-applications?status=PENDING&page=0&size=10 → 상태별 목록
// POST /api/admin/owner-applications/{id}/approve                  → 승인 (회원 역할 USER → OWNER)
//   404: 신청 없음, 409: 이미 처리된 신청 · 같은 사업자번호로 이미 승인된 다른 회원이 있음(실제 신청만)
// POST /api/admin/owner-applications/{id}/reject  { "reason": "" } → 반려
//   400: 사유 없음 · 500자 초과, 404: 신청 없음, 409: 이미 처리된 신청
// 화면: admin/owner-applications.html (테스트 신청을 승인할 때는 경고 창을 따로 띄움)
//
// 보안: SecurityConfig 의 "/api/admin/**" → hasRole("ADMIN") 에 해당 (수정 불필요)
// 응답 모양은 AdminApiController 목록과 같음 { content, totalPages, totalElements, currentPage }
// → admin 화면의 renderPagination 을 그대로 쓸 수 있음
// AdminApiController(다른 팀원 파일)를 수정하지 않으려고 별도 컨트롤러로 분리
@RestController
@RequestMapping("/api/admin/owner-applications")
@RequiredArgsConstructor
public class OwnerApplicationAdminApiController {

    private final OwnerApplicationService ownerApplicationService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "PENDING") OwnerApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<OwnerApplication> result = ownerApplicationService.findByStatus(status, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("content", result.getContent().stream().map(OwnerApplicationResponse::from).toList());
        response.put("totalPages", result.getTotalPages());
        response.put("totalElements", result.getTotalElements());
        response.put("currentPage", result.getNumber());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable Long id) {
        try {
            ownerApplicationService.approve(id);
            return ResponseEntity.ok(Map.of("message", "승인되었습니다. 신청자의 역할이 식당 사장으로 변경되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> reject(@PathVariable Long id,
                                                      @RequestBody(required = false) Map<String, String> body) {
        try {
            String reason = body == null ? null : body.get("reason");
            ownerApplicationService.reject(id, reason);
            return ResponseEntity.ok(Map.of("message", "반려되었습니다."));
        } catch (IllegalArgumentException e) {
            // 사유 누락/길이 초과 → 400, 신청 없음 → 404
            HttpStatus status = "신청을 찾을 수 없습니다.".equals(e.getMessage())
                    ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }
}
