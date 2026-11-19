package com.example.mymatzip.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class EmailAuthService {

    private final JavaMailSender mailSender;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long EXPIRE_MINUTES = 5;

    // 이메일 -> (인증번호, 만료시간)
    private final Map<String, AuthCode> codeStorage = new ConcurrentHashMap<>();
    // 인증에 성공한 이메일 임시 보관 (회원가입 제출 시 최종 검증용)
    private final Set<String> verifiedEmails = ConcurrentHashMap.newKeySet();

    private record AuthCode(String code, LocalDateTime expiresAt) {
        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    // 1. 6자리 인증번호 생성 및 메일 발송
    public void sendCode(String email) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[부산 한끼] 이메일 인증번호 안내");
        message.setText("인증번호 6자리: " + code + "\n5분 이내에 입력해주세요.");
        mailSender.send(message);

        codeStorage.put(email, new AuthCode(code, LocalDateTime.now().plusMinutes(EXPIRE_MINUTES)));
        verifiedEmails.remove(email);
    }

    // 2. 인증번호 검증
    public boolean verifyCode(String email, String inputCode) {
        AuthCode saved = codeStorage.get(email);
        if (saved == null || saved.isExpired()) {
            codeStorage.remove(email);
            return false;
        }
        if (!saved.code().equals(inputCode)) {
            return false;
        }
        codeStorage.remove(email); // 일회용 처리
        verifiedEmails.add(email); // 인증 완료 기록
        return true;
    }

    // 3. 최종 회원가입 시 인증 완료 여부 확인후 소모
    public boolean consumeVerifiedEmail(String email) {
        return verifiedEmails.remove(email);
    }



}