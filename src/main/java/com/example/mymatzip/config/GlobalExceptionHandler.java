/*
 * ============================================================================
 *  파일명    : GlobalExceptionHandler.java
 *  위치      : com.example.mymatzip.config (설정 패키지)
 *  역할      : 애플리케이션 전체에서 발생하는 예외(에러)를 한 곳에서 처리하는 "에러 총괄 매니저"
 * ============================================================================
 *
 *
 *  ■ MVC에서의 위치
 *    Controller 계층에서 발생하는 예외를 가로채서 처리합니다.
 *    [Controller에서 예외 발생] → [이 클래스가 가로챔] → [깔끔한 에러 응답 반환]
 *
 *  ■ 처리하는 예외 종류
 *    1) MethodArgumentNotValidException: @Valid 유효성 검증 실패 시 발생
 *       (예: 이메일 형식이 틀렸거나, 필수 값이 비어있을 때)
 *    2) IllegalArgumentException: 잘못된 인자(값)가 전달되었을 때 발생
 *       (예: 존재하지 않는 회원 ID, 이미 사용 중인 이메일 등)
 *
 *  ■ URL 매핑
 *    이 파일은 특정 URL에 매핑되지 않습니다.
 *    모든 @RestController에서 발생하는 예외를 자동으로 가로채서 처리합니다.
 * ============================================================================
 */

// ── 패키지 선언 ──────────────────────────────────────────────
// 이 파일이 config(설정) 패키지에 속함을 선언합니다.
package com.example.mymatzip.config;

// ── import 문 (외부 라이브러리 가져오기) ─────────────────────

// ResponseEntity: HTTP 응답을 구성하는 객체
// 상태 코드(200, 400, 404 등), 헤더, 본문(body)을 모두 설정할 수 있습니다.
import org.springframework.http.ResponseEntity;

// MethodArgumentNotValidException: @Valid 어노테이션으로 유효성 검사를 했을 때
// 검증에 실패하면 자동으로 발생하는 예외(에러)
// 예: @Email이 붙은 필드에 "abc"를 넣으면 이 예외가 발생
import org.springframework.web.bind.MethodArgumentNotValidException;

// @ExceptionHandler: 특정 예외가 발생했을 때 이 메서드가 처리하겠다고 표시하는 어노테이션
import org.springframework.web.bind.annotation.ExceptionHandler;

// @RestControllerAdvice: 모든 @RestController에 적용되는 "전역 조언자"
// @ControllerAdvice + @ResponseBody를 합친 것
// 모든 REST 컨트롤러에서 발생하는 예외를 이 클래스가 가로채서 처리합니다.
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Map: 키-값 쌍으로 데이터를 저장하는 자바 자료구조
// 예: {"error": "이메일 형식이 올바르지 않습니다."} 형태의 JSON 응답을 만들 때 사용
import java.util.Map;

// ── @RestControllerAdvice 어노테이션 ────────────────────────
// 이 클래스가 모든 @RestController의 예외를 전역적으로 처리하는 "조언자"임을 선언합니다.
// "RestController" → REST API 컨트롤러에서 발생하는 예외만 처리
// "Advice" → "조언/보조" 역할 (컨트롤러를 보조하여 에러 처리를 대신 해줌)
@RestControllerAdvice
// ── 클래스 선언 ─────────────────────────────────────────────
// public: 다른 패키지에서도 접근 가능
// class GlobalExceptionHandler: "전역 예외 처리기"라는 이름의 클래스
public class GlobalExceptionHandler {

    // ── handleValidation 메서드 ─────────────────────────────
    // ■ 역할: @Valid 유효성 검증 실패 시 발생하는 예외를 처리
    // ■ 언제 호출되나?
    //   Controller에서 @Valid @RequestBody로 입력값을 검증할 때,
    //   DTO의 @NotBlank, @Email, @Size 등 조건에 맞지 않으면
    //   MethodArgumentNotValidException이 발생하고, 이 메서드가 자동 호출됩니다.
    //
    // ■ 예시 상황:
    //   회원가입 시 이메일을 "abc"로 보냄 → @Email 검증 실패 →
    //   이 메서드가 호출됨 → {"error": "email: 올바른 이메일 형식이 아닙니다."} 응답
    //
    // ■ 매개변수: e → 발생한 예외 객체 (어떤 필드에서 어떤 에러가 났는지 정보 포함)
    // ■ 반환값: ResponseEntity<Map<String, String>>
    //   → HTTP 400(Bad Request) 상태코드와 함께 {"error": "에러 메시지"} JSON 반환
    //
    // @ExceptionHandler(MethodArgumentNotValidException.class):
    //   "MethodArgumentNotValidException이 발생하면 이 메서드가 처리한다"고 선언
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {

        // ── 1단계: 에러 메시지 추출 ─────────────────────────
        // e.getBindingResult(): 유효성 검증 결과를 가져옴
        // .getFieldErrors(): 검증에 실패한 필드(항목)들의 목록을 가져옴
        //   예: [{field: "email", message: "올바른 형식이 아닙니다"}, ...]
        // .stream(): 목록을 스트림(흐름)으로 변환 (자바 8의 함수형 프로그래밍)
        // .map(error -> ...): 각 에러를 "필드명: 에러메시지" 형태의 문자열로 변환
        //   예: "email: 올바른 이메일 형식이 아닙니다."
        // .findFirst(): 첫 번째 에러 메시지만 가져옴 (여러 개 중 하나만)
        // .orElse("..."): 만약 에러가 하나도 없으면 기본 메시지 사용
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("입력값이 올바르지 않습니다.");

        // ── 2단계: HTTP 400 응답 반환 ───────────────────────
        // ResponseEntity.badRequest(): HTTP 상태코드 400(Bad Request)으로 설정
        //   400 = "요청이 잘못되었습니다" (클라이언트의 실수)
        // .body(Map.of("error", message)): 응답 본문에 에러 메시지를 JSON으로 담음
        //   Map.of("error", message) → {"error": "email: 올바른 이메일 형식이 아닙니다."}
        //   브라우저(JavaScript)에서 이 JSON을 읽어서 에러 메시지를 화면에 표시합니다.
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    // ── handleIllegalArgument 메서드 ────────────────────────
    // ■ 역할: 잘못된 인자(값)가 전달되었을 때 발생하는 예외를 처리
    // ■ 언제 호출되나?
    //   Service 계층에서 비즈니스 로직 검증 시 throw new IllegalArgumentException()으로
    //   예외를 던지면, Controller를 거쳐 이 메서드가 자동 호출됩니다.
    //
    // ■ 예시 상황:
    //   - 이미 사용 중인 이메일로 회원가입 시도 → "이미 사용 중인 이메일입니다." 에러
    //   - 존재하지 않는 맛집 ID로 조회 시도 → "해당 맛집을 찾을 수 없습니다." 에러
    //
    // ■ 매개변수: e → 발생한 예외 객체 (에러 메시지 포함)
    // ■ 반환값: ResponseEntity<Map<String, String>>
    //   → HTTP 400(Bad Request) 상태코드와 함께 {"error": "에러 메시지"} JSON 반환
    //
    // @ExceptionHandler(IllegalArgumentException.class):
    //   "IllegalArgumentException이 발생하면 이 메서드가 처리한다"고 선언
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {

        // e.getMessage(): 예외를 던질 때 설정한 에러 메시지를 가져옴
        //   예: throw new IllegalArgumentException("이미 사용 중인 이메일입니다.")
        //       → e.getMessage() = "이미 사용 중인 이메일입니다."
        // ResponseEntity.badRequest(): HTTP 400 상태코드
        // .body(Map.of("error", e.getMessage())):
        //   {"error": "이미 사용 중인 이메일입니다."} 형태의 JSON 응답
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}