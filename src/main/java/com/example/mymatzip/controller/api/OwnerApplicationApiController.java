package com.example.mymatzip.controller.api;

import com.example.mymatzip.config.security.LoginMember;
import com.example.mymatzip.dto.OwnerApplicationForm;
import com.example.mymatzip.dto.OwnerApplicationResponse;
import com.example.mymatzip.entity.Member;
import com.example.mymatzip.entity.OwnerApplication;
import com.example.mymatzip.service.OwnerApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

// ============================================================
// [OwnerApplicationApiController.java] 식당 사장 신청 API (로그인한 회원용)
// ============================================================
//
// GET  /api/owner-applications/me → { "application": 내 가장 최근 신청 또는 null, "currentRole": DB 의 현재 역할 }
// POST /api/owner-applications    → 신청 (201)
//
// 보안: SecurityConfig 의 anyRequest().authenticated() 에 해당 → 로그인 필요 (수정 불필요)
@RestController
@RequestMapping("/api/owner-applications")
@RequiredArgsConstructor
public class OwnerApplicationApiController {

    private final OwnerApplicationService ownerApplicationService;

    @GetMapping("/me")
    public ResponseEntity<?> myApplication(@LoginMember Member loginMember) {
        if (loginMember == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인이 필요합니다."));
        }
        // Map.of 는 null 값을 넣을 수 없어서 HashMap 사용
        Map<String, Object> body = new HashMap<>();
        body.put("application", ownerApplicationService.findMyLatest(loginMember.getId())
                .map(OwnerApplicationResponse::from)
                .orElse(null));
        // 세션 값이 아닌 DB 의 현재 역할 (관리자가 역할을 바꿔도 바로 반영)
        body.put("currentRole", ownerApplicationService.findCurrentRole(loginMember.getId()).name());
        return ResponseEntity.ok(body);
    }

    @PostMapping
    public ResponseEntity<?> apply(@LoginMember Member loginMember,
                                   @Valid @RequestBody OwnerApplicationForm form) {
        if (loginMember == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인이 필요합니다."));
        }
        try {
            OwnerApplication saved = ownerApplicationService.apply(loginMember.getId(), form);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(OwnerApplicationResponse.from(saved));
        } catch (IllegalStateException e) {
            // 이미 심사 중 / 이미 OWNER·ADMIN
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
        // IllegalArgumentException 은 GlobalExceptionHandler 가 400 으로 처리
    }
}
