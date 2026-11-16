// ============================================================
// [MemberSignupForm.java] 회원가입 요청 데이터 전달 객체 (DTO)
// ============================================================
// 역할: 사용자가 회원가입 화면에서 입력한 이메일, 비밀번호, 닉네임을 담는 "택배 상자"
//
// DTO(Data Transfer Object)란?
//
// DTO vs Entity 차이:
//   Entity = DB 테이블과 직접 매핑되는 핵심 객체 (@Setter 금지, 불변 설계)
//
// 사용 흐름:
//   1) 사용자가 회원가입 폼에 이메일/비밀번호/닉네임 입력
//   2) JavaScript의 fetch()가 JSON으로 서버에 전송
//      예: { "email": "hong@mail.com", "password": "1234", "nickname": "맛집탐험가" }
//   3) Spring이 JSON을 MemberSignupForm 객체로 자동 변환 (@RequestBody)
//   4) @Valid로 검증 → 통과하면 MemberService.signup()에 전달
//   5) MemberService에서 중복 확인 후 비밀번호 암호화 → DB에 저장
//
// 검증(Validation) 흐름:
//   - @NotBlank : 빈 문자열("") 또는 공백만 있는 문자열("  ") 거부
//   - @Email    : 이메일 형식(xxx@xxx.xxx) 검증
//   - @Size     : 문자열 최소/최대 길이 검증
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
import jakarta.validation.constraints.Email;

// @NotBlank 어노테이션 가져오기 — 값이 비어있지 않은지 검증하는 어노테이션
// null, 빈 문자열(""), 공백만 있는 문자열("  ") 모두 거부합니다.
import jakarta.validation.constraints.NotBlank;

// @Size 어노테이션 가져오기 — 문자열의 최소/최대 길이를 검증하는 어노테이션
// 예: @Size(min = 4, max = 20) → 4글자 이상, 20글자 이하만 허용
import jakarta.validation.constraints.Size;

// @Getter 가져오기 — Lombok 라이브러리가 제공하는 어노테이션
// 모든 필드에 대해 getter 메서드를 자동 생성합니다.
// 예: getEmail(), getPassword(), getNickname()
import lombok.Getter;

// @Setter 가져오기 — Lombok 라이브러리가 제공하는 어노테이션
// 모든 필드에 대해 setter 메서드를 자동 생성합니다.
// 예: setEmail(), setPassword(), setNickname()
// Spring이 JSON → 객체 변환 시 setter를 사용하므로 DTO에는 필수입니다.
import lombok.Setter;

// @Getter 어노테이션 — 모든 필드에 대해 getter 메서드를 자동 생성
// 이 어노테이션이 있으면 아래와 같은 코드가 자동으로 만들어집니다:
//   public String getEmail() { return this.email; }
//   public String getPassword() { return this.password; }
//   public String getNickname() { return this.nickname; }
@Getter

// @Setter 어노테이션 — 모든 필드에 대해 setter 메서드를 자동 생성
// 이 어노테이션이 있으면 아래와 같은 코드가 자동으로 만들어집니다:
//   public void setEmail(String email) { this.email = email; }
//   public void setPassword(String password) { this.password = password; }
//   public void setNickname(String nickname) { this.nickname = nickname; }
@Setter

// MemberSignupForm 클래스 선언
// DTO(Data Transfer Object) = 회원가입 데이터를 전달하기 위한 "택배 상자" 역할의 클래스
public class MemberSignupForm {

    // ──────────────────────────────────────────────────────
    // 이메일 필드
    // ──────────────────────────────────────────────────────
    // [타입] String — 문자열 (예: "hong@mail.com")
    // [용도] 사용자가 회원가입 폼에 입력한 이메일 주소를 저장
    // [DB 매핑] → Member 엔티티의 email 필드로 전달되어 DB에 저장됨
    // [제약조건] 중복 불가 — MemberService에서 existsByEmail()로 중복 확인
    //
    // @NotBlank: 빈 값 검증 → null, "", "  " 모두 거부
    // @Email: 이메일 형식 검증 → xxx@xxx.xxx 형식이 아니면 거부
    // ──────────────────────────────────────────────────────
    @NotBlank(message = "이메일을 입력해주세요.")        // 빈 값 검증 — 비어있으면 이 메시지를 에러로 반환
    @Email(message = "올바른 이메일 형식이 아닙니다.")    // 이메일 형식 검증 — xxx@xxx.xxx 형식이 아니면 에러
    private String email;                               // 회원가입 이메일 주소 (예: "hong@mail.com")

    // ──────────────────────────────────────────────────────
    // 비밀번호 필드
    // ──────────────────────────────────────────────────────
    // [타입] String — 문자열 (예: "mypassword1234")
    // [용도] 사용자가 회원가입 폼에 입력한 비밀번호를 저장
    // [주의] 이 값은 평문(암호화 전)이며, MemberService에서 BCrypt로 암호화 후 DB에 저장됨
    // [DB 매핑] → Member 엔티티의 password 필드 (암호화된 상태로 저장)
    //
    // @NotBlank: 빈 값 검증 → null, "", "  " 모두 거부
    // @Size(min=4, max=20): 길이 검증 → 최소 4자, 최대 20자만 허용
    //   → 4자 미만이면 "비밀번호는 4~20자로 입력해주세요." 에러 메시지 반환
    //   → 20자 초과해도 같은 에러 메시지 반환
    // ──────────────────────────────────────────────────────
    @NotBlank(message = "비밀번호를 입력해주세요.")                          // 빈 값 검증 — 비어있으면 에러
    @Size(min = 4, max = 20, message = "비밀번호는 4~20자로 입력해주세요.")  // 길이 검증 — 4~20자 범위
    private String password;                                               // 회원가입 비밀번호 (평문 상태, 예: "mypassword1234")

    // ──────────────────────────────────────────────────────
    // 닉네임 필드
    // ──────────────────────────────────────────────────────
    // [타입] String — 문자열 (예: "맛집탐험가")
    // [용도] 사용자가 회원가입 폼에 입력한 닉네임(별명)을 저장
    // [DB 매핑] → Member 엔티티의 nickname 필드로 전달되어 DB에 저장됨
    // [제약조건] 중복 불가 — MemberService에서 existsByNickname()으로 중복 확인
    //
    // @NotBlank: 빈 값 검증 → null, "", "  " 모두 거부
    // @Size(min=2, max=10): 길이 검증 → 최소 2자, 최대 10자만 허용
    //   → 2자 미만이면 "닉네임은 2~10자로 입력해주세요." 에러 메시지 반환
    //   → 10자 초과해도 같은 에러 메시지 반환
    // ──────────────────────────────────────────────────────
    @NotBlank(message = "닉네임을 입력해주세요.")                            // 빈 값 검증 — 비어있으면 에러
    @Size(min = 2, max = 10, message = "닉네임은 2~10자로 입력해주세요.")    // 길이 검증 — 2~10자 범위
    private String nickname;                                               // 회원가입 닉네임 (예: "맛집탐험가")
}