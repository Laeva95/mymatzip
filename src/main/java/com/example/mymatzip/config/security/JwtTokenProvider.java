/*
 * ============================================================================
 *  파일명    : JwtTokenProvider.java
 *  위치      : com.example.mymatzip.config.security (보안 설정 패키지)
 *  역할      : JWT(JSON Web Token) 토큰의 생성, 검증, 파싱을 담당하는 "토큰 발급기"
 * ============================================================================
 *
 *  ■ JWT란?
 *    JSON Web Token의 약자로, 사용자 인증 정보를 담은 "디지털 신분증"입니다.
 *    서버가 발급하고, 클라이언트가 보관하며, 요청 시 함께 보냅니다.
 *
 *
 *  ■ JWT의 구조 (마침표로 3부분 구분)
 *    xxxxx.yyyyy.zzzzz
 *    ① Header (헤더): 토큰 타입(JWT), 서명 알고리즘(HS256)
 *    ② Payload (내용): 사용자 정보(id, email, role), 만료 시간
 *    ③ Signature (서명): 위변조 방지용 서명 (비밀키로 암호화)
 *
 *  ■ 세션(Session)과의 차이
 *    세션: 서버에 사용자 정보 저장 → 서버 메모리 사용 → 서버 확장 어려움
 *    JWT: 클라이언트에 토큰 저장 → 서버 부담 없음 → 서버 확장 용이
 *    이 앱에서는 둘 다 사용합니다:
 *    - 타임리프 화면: 세션 기반 인증 (기존 방식 유지)
 *    - REST API: JWT 기반 인증 (모바일 앱 등 대비)
 *
 *  ■ 사용처
 *    - MemberApiController: 로그인 성공 시 JWT 토큰 발급
 *    - JwtAuthenticationFilter: API 요청 시 JWT 토큰 검증
 * ============================================================================
 */

// ── 패키지 선언 ──────────────────────────────────────────────
package com.example.mymatzip.config.security;

// ── import 문 ────────────────────────────────────────────────

// Member: 회원 엔티티 — JWT에 담을 회원 정보의 원본
import com.example.mymatzip.entity.Member;

// JJWT 라이브러리: JWT 토큰을 생성하고 파싱(해석)하는 라이브러리
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

// @Value: application.properties의 설정값을 주입받는 어노테이션
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// 날짜 관련 import
import java.util.Date;

// 암호화 키 관련 import
import javax.crypto.SecretKey;

// [수정] 비밀키가 없을 때 임의 키를 만들고 경고 로그를 남기기 위한 import
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// ── @Component ───────────────────────────────────────────────
// 이 클래스를 스프링 빈으로 등록합니다.
// 다른 클래스에서 의존성 주입(DI)으로 사용할 수 있습니다.
@Component
public class JwtTokenProvider {

    // ── 필드 (설정값) ────────────────────────────────────────

    // JWT 서명에 사용할 비밀키 (application.properties에서 가져옴)
    // 이 키를 알아야 토큰을 생성/검증할 수 있으므로 절대 외부에 노출하면 안 됩니다!
    // @Value: application.properties의 jwt.secret 값을 이 필드에 주입
    private final SecretKey secretKey;

    // JWT 토큰의 유효 기간 (밀리초 단위, application.properties에서 가져옴)
    // 86400000ms = 24시간 = 1일
    private final long expiration;

    // ── 생성자 ───────────────────────────────────────────────
    // @Value로 application.properties의 값을 주입받는 생성자
    // secret: jwt.secret 속성값 (문자열)
    // expiration: jwt.expiration 속성값 (숫자)
    //
    // Keys.hmacShaKeyFor(): 문자열 비밀키를 HMAC-SHA256 암호화에 사용할 수 있는
    //   SecretKey 객체로 변환합니다.
    //
    // [수정] jwt.secret 은 환경 변수 JWT_SECRET 에서 받습니다(application.properties 참고).
    //   값이 비어 있으면 실행할 때마다 64바이트 임의 키를 만듭니다(개발용).
    //   이때 서버를 다시 켜면 예전에 발급한 토큰은 검증에 실패합니다.
    //   값이 32바이트보다 짧으면 Keys.hmacShaKeyFor() 가 WeakKeyException 으로 시작을 막습니다.
    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    public JwtTokenProvider(@Value("${jwt.secret:}") String secret,
                            @Value("${jwt.expiration}") long expiration) {
        if (secret == null || secret.isBlank()) {
            byte[] randomKey = new byte[64];
            new SecureRandom().nextBytes(randomKey);
            this.secretKey = Keys.hmacShaKeyFor(randomKey);
            log.warn("JWT_SECRET 환경 변수가 없어 임의 JWT 비밀키를 사용합니다. 서버를 다시 켜면 기존 토큰은 무효가 됩니다.");
        } else {
            this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        }
        this.expiration = expiration;
    }

    // ── generateToken 메서드 ─────────────────────────────────
    // 역할: 회원 정보를 담은 JWT 토큰을 생성합니다.
    //
    // 매개변수: Member member — 토큰에 담을 회원 정보
    // 반환값: String — 생성된 JWT 토큰 문자열
    //
    // 토큰에 담기는 정보 (Claims):
    //   - subject: 회원 ID (토큰의 "주인"을 식별)
    //   - email: 회원 이메일
    //   - role: 회원 역할 (USER 또는 ADMIN)
    //   - issuedAt: 토큰 발급 시간
    //   - expiration: 토큰 만료 시간
    //
    public String generateToken(Member member) {
        // 현재 시간 (토큰 발급 시간으로 사용)
        Date now = new Date();

        // 만료 시간 = 현재 시간 + 유효 기간(24시간)
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                // subject: 토큰의 주체(주인) — 회원 ID를 문자열로 저장
                .subject(String.valueOf(member.getId()))
                // claim: 추가 정보(클레임) — 이메일과 역할을 저장
                .claim("email", member.getEmail())
                .claim("role", member.getRole().name())
                // issuedAt: 토큰이 발급된 시간
                .issuedAt(now)
                // expiration: 토큰이 만료되는 시간
                .expiration(expiryDate)
                // signWith: 비밀키로 서명 (위변조 방지)
                .signWith(secretKey)
                // compact: 최종 JWT 문자열로 변환
                .compact();
    }

    // ── validateToken 메서드 ─────────────────────────────────
    // 역할: JWT 토큰이 유효한지 검증합니다.
    //
    // 매개변수: String token — 검증할 JWT 토큰 문자열
    // 반환값: boolean — 유효하면 true, 유효하지 않으면 false
    //
    // 검증 내용:
    //   1) 서명이 올바른가? (위조/변조되지 않았는가?)
    //   2) 만료되지 않았는가?
    //   3) 형식이 올바른가?
    //
    public boolean validateToken(String token) {
        try {
            // 토큰을 파싱(해석)하여 검증 — 문제가 있으면 예외 발생
            Jwts.parser()
                    .verifyWith(secretKey)   // 서명 검증에 사용할 비밀키 설정
                    .build()
                    .parseSignedClaims(token); // 토큰 파싱 + 서명 검증
            return true; // 문제 없으면 유효한 토큰
        } catch (JwtException | IllegalArgumentException e) {
            // 서명 불일치, 토큰 만료, 형식 오류 등 → 유효하지 않은 토큰
            return false;
        }
    }

    // ── getMemberIdFromToken 메서드 ──────────────────────────
    // 역할: JWT 토큰에서 회원 ID를 추출합니다.
    //
    // 매개변수: String token — 파싱할 JWT 토큰 문자열
    // 반환값: Long — 토큰에 저장된 회원 ID
    //
    public Long getMemberIdFromToken(String token) {
        // 토큰을 파싱하여 Claims(정보 묶음)를 가져옴
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // subject(주체)에 저장한 회원 ID를 Long으로 변환하여 반환
        return Long.parseLong(claims.getSubject());
    }
}