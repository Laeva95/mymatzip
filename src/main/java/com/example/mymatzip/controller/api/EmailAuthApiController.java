package com.example.mymatzip.controller.api;

import com.example.mymatzip.service.EmailAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailAuthApiController {

    private  final EmailAuthService emailAuthService;

    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> send(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "이메일을 입력해주세요."));
        }
        emailAuthService.sendCode(email.trim());
        return ResponseEntity.ok(Map.of("message", "인증번호 6자리가 발송되었습니다."));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");

        if (emailAuthService.verifyCode(email, code)) {
            return ResponseEntity.ok(Map.of("verified", true, "message", "이메일 인증이 완료되었습니다."));
        }
        return ResponseEntity.badRequest().body(Map.of("verified", false, "error", "인증번호가 틀렸거나 만료되었습니다."));
    }


}
