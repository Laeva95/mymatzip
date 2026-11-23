package com.example.mymatzip.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// ============================================================
// [OwnerApplicationForm.java] 식당 사장 권한 신청 요청 데이터
// ============================================================
// 사용처: POST /api/owner-applications (OwnerApplicationApiController.apply)
// @Valid 검증에 실패하면 GlobalExceptionHandler 가 400 + {"error": "필드: 메시지"} 로 응답
@Getter
@Setter
public class OwnerApplicationForm {

    @NotBlank(message = "가게 이름을 입력해주세요.")
    @Size(max = 100, message = "가게 이름은 100자 이내로 입력해주세요.")
    private String storeName;

    @NotBlank(message = "가게 주소를 입력해주세요.")
    @Size(max = 255, message = "주소는 255자 이내로 입력해주세요.")
    private String address;

    // 허용 예
    //   지역번호·휴대폰 : 02-123-4567, 051-123-4567, 010-1234-5678, 0511234567
    //   안심번호(0507 등): 0507-1234-5678
    //   대표번호(15xx~19xx): 1588-1234, 1577-1234
    @NotBlank(message = "연락처를 입력해주세요.")
    @Pattern(regexp = "^(0\\d{1,3}-?\\d{3,4}-?\\d{4}|1[5-9]\\d{2}-?\\d{4})$", message = "연락처 형식이 올바르지 않습니다.")
    private String phone;

    // 예: 123-45-67890 (하이픈 포함 12자리)
    @NotBlank(message = "사업자등록번호를 입력해주세요.")
    @Pattern(regexp = "^\\d{3}-\\d{2}-\\d{5}$", message = "사업자등록번호는 000-00-00000 형식으로 입력해주세요.")
    private String businessNumber;

    // ── 국세청 진위확인용 (저장하지 않음) ──────────────────────
    // 사업자등록증에 적힌 그대로 입력해야 일치로 확인됨
    @NotBlank(message = "대표자 성명을 입력해주세요.")
    @Size(max = 50, message = "대표자 성명은 50자 이내로 입력해주세요.")
    private String ownerName;

    // <input type="date"> 값 형식 (예: 2020-03-15). 국세청에는 20200315 로 보냄
    @NotBlank(message = "개업일자를 입력해주세요.")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "개업일자 형식이 올바르지 않습니다.")
    private String openDate;

    // 대표자 성명을 외부(국세청 API)로 보내므로 동의를 받음 (테스트 신청이면 외부로 보내지 않으므로 필요 없음)
    private boolean agreeBusinessCheck;

    // 시연 · 테스트용 신청: 국세청 API 대신 테스트 번호표로 결과를 정함 (NtsBusinessApiClient.verifyForTest)
    private boolean testMode;

    // 동의 검사: 실제 확인(testMode=false)일 때만 동의 필수
    @AssertTrue(message = "사업자 정보 확인(국세청 조회)에 동의해주세요.")
    public boolean isBusinessCheckAgreed() {
        return testMode || agreeBusinessCheck;
    }

    @Size(max = 1000, message = "남기는 말은 1000자 이내로 입력해주세요.")
    private String message; // 선택 입력
}
