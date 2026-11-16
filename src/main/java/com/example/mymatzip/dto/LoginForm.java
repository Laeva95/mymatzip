// ============================================================
// [LoginForm.java] 로그인 요청 데이터 전달 객체 (DTO)
// ============================================================
// 역할: 사용자가 로그인 화면에서 입력한 이메일과 비밀번호를 담는 "택배 상자"
//
// DTO(Data Transfer Object)란?
//
// DTO vs Entity 차이:
//   Entity = DB 테이블과 직접 매핑되는 핵심 객체 (@Setter 금지, 불변 설계)
//
// 사용 흐름:
//   1) 사용자가 로그인 폼에 이메일/비밀번호 입력
//   2) JavaScript의 fetch()가 JSON으로 서버에 전송
//      예: { "email": "hong@mail.com", "password": "1234" }
//   3) Spring이 JSON을 LoginForm 객체로 자동 변환 (@RequestBody)
//   4) @Valid로 검증 → 통과하면 MemberService.login()에 전달
//   5) MemberService에서 DB 조회 후 비밀번호 일치 여부 확인
//
// 검증(Validation) 흐름:
//   - @NotBlank : 빈 문자열("") 또는 공백만 있는 문자열("  ") 거부
//   - @Email    : 이메일 형식(xxx@xxx.xxx) 검증
//   - Controller에서 @Valid와 함께 사용하면 자동으로 검증됨
//   - 검증 실패 시 message에 적힌 에러 메시지가 사용자에게 표시됨
// ============================================================

// --- 패키지 선언 ---
// 이 파일이 속한 폴더(패키지) 경로를 지정합니다.
// com.example.mymatzip.dto = "DTO(데이터 전달 객체)" 계층에 해당
package com.example.mymatzip.dto;

// --- import 영역 (외부 클래스 가져오기) ---

// @Email 어노테이션 가져오기 — 이메일 형식인지 검증하는 어노테이션
// 예: "hong@mail.com" → 통과 / "hongmail" → 실패 (@ 없음)
// Jakarta Bean Validation API에서 제공하는 표준 검증 어노테이션입니다.
import jakarta.validation.constraints.Email;

// @NotBlank 어노테이션 가져오기 — 값이 비어있지 않은지 검증하는 어노테이션
// null, 빈 문자열(""), 공백만 있는 문자열("  ") 모두 거부합니다.
// @NotNull(null만 거부)보다 엄격하고, @NotEmpty(빈 문자열만 거부)보다 더 엄격합니다.
import jakarta.validation.constraints.NotBlank;

// @Getter 가져오기 — Lombok 라이브러리가 제공하는 어노테이션
// 모든 필드에 대해 getter 메서드(getEmail(), getPassword())를 자동 생성합니다.
import lombok.Getter;

// @Setter 가져오기 — Lombok 라이브러리가 제공하는 어노테이션
// 모든 필드에 대해 setter 메서드(setEmail(), setPassword())를 자동 생성합니다.
// ★ DTO에서는 @Setter 사용 가능 (Entity에서는 사용 금지!)
import lombok.Setter;

// @Getter 어노테이션 — 모든 필드에 대해 getter 메서드를 자동 생성
// 이 어노테이션이 있으면 아래와 같은 코드가 자동으로 만들어집니다:
//   public String getEmail() { return this.email; }
//   public String getPassword() { return this.password; }
@Getter

// @Setter 어노테이션 — 모든 필드에 대해 setter 메서드를 자동 생성
// 이 어노테이션이 있으면 아래와 같은 코드가 자동으로 만들어집니다:
//   public void setEmail(String email) { this.email = email; }
//   public void setPassword(String password) { this.password = password; }
// Spring이 JSON → 객체 변환 시 setter를 사용하므로 DTO에는 필수입니다.
@Setter

// LoginForm 클래스 선언
// DTO(Data Transfer Object) = 데이터를 전달하기 위한 "택배 상자" 역할의 클래스
public class LoginForm {

    // ──────────────────────────────────────────────────────
    // 이메일 필드
    // ──────────────────────────────────────────────────────
    // [타입] String — 문자열 (예: "hong@mail.com")
    // [용도] 사용자가 로그인 폼에 입력한 이메일 주소를 저장
    // [DB 매핑] Member 엔티티의 email 필드와 대응 (직접 매핑은 아님, 조회용)
    //
    // @NotBlank: 이 필드는 반드시 값이 있어야 함
    //   - null 불가, 빈 문자열("") 불가, 공백만("  ") 불가
    //   - 검증 실패 시 "이메일을 입력해주세요." 메시지가 에러로 반환됨
    //
    // @Email: 이메일 형식이어야 함
    //   - "hong@mail.com" → 통과
    //   - "hongmail" → 실패 (@ 기호가 없음)
    //   - 검증 실패 시 "올바른 이메일 형식이 아닙니다." 메시지가 에러로 반환됨
    //
    // [검증 순서] @NotBlank가 먼저 체크되고, 통과하면 @Email이 체크됨
    // ──────────────────────────────────────────────────────
    @NotBlank(message = "이메일을 입력해주세요.")        // 빈 값 검증 — 비어있으면 이 메시지를 에러로 반환
    @Email(message = "올바른 이메일 형식이 아닙니다.")    // 이메일 형식 검증 — xxx@xxx.xxx 형식이 아니면 에러
    private String email;                               // 로그인 이메일 주소 (예: "hong@mail.com")

    // ──────────────────────────────────────────────────────
    // 비밀번호 필드
    // ──────────────────────────────────────────────────────
    // [타입] String — 문자열 (예: "mypassword1234")
    // [용도] 사용자가 로그인 폼에 입력한 비밀번호를 저장
    // [주의] 이 값은 평문(암호화 전)이며, Service에서 암호화된 DB 값과 비교합니다.
    //
    // @NotBlank: 이 필드는 반드시 값이 있어야 함
    //   - null 불가, 빈 문자열("") 불가, 공백만("  ") 불가
    //   - 검증 실패 시 "비밀번호를 입력해주세요." 메시지가 에러로 반환됨
    //
    // [로그인 시에는 @Size 검증이 없는 이유]
    //   회원가입 시에는 비밀번호 길이를 제한하지만,
    //   로그인 시에는 입력한 비밀번호가 DB와 일치하는지만 확인하면 되므로
    //   길이 제한이 필요 없습니다.
    // ──────────────────────────────────────────────────────
    @NotBlank(message = "비밀번호를 입력해주세요.")      // 빈 값 검증 — 비어있으면 이 메시지를 에러로 반환
    private String password;                            // 로그인 비밀번호 (평문 상태, 예: "mypassword1234")
}