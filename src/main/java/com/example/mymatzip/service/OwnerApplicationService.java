package com.example.mymatzip.service;

import com.example.mymatzip.dto.OwnerApplicationForm;
import com.example.mymatzip.entity.Member;
import com.example.mymatzip.entity.OwnerApplication;
import com.example.mymatzip.entity.OwnerApplicationStatus;
import com.example.mymatzip.entity.Role;
import com.example.mymatzip.repository.OwnerApplicationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

// ============================================================
// [OwnerApplicationService.java] 식당 사장 권한 신청 · 심사
// ============================================================
//
// 예외 규칙 (컨트롤러가 HTTP 상태로 바꿔서 응답)
//   IllegalArgumentException → 대상 없음 / 입력 오류       → 400 또는 404
//   IllegalStateException    → 지금 상태에서 할 수 없는 요청 → 409
//     예: 이미 심사 중인데 또 신청, 이미 OWNER 인데 신청, 이미 처리된 신청을 다시 승인
//   NtsBusinessApiClient.UnavailableException → 국세청 확인 불가 → 503
//
// 사업자번호 검증 (신청할 때)
//   1단계 형식 · 체크섬 : 마지막 자리 검증 숫자가 맞는지 (존재할 수 없는 번호를 API 호출 없이 거름)
//   2단계 국세청 API    : 사업자번호 + 개업일자 + 대표자 성명 일치 여부, 계속사업자(영업 중)인지
//   그다음 관리자 심사(3단계)에서 가게 대표번호로 확인 전화 등으로 실제 사장인지 확인
//   테스트 신청(form.testMode, 시연용): 2단계를 국세청 대신 테스트 번호표로 처리하고 testApplication = true 로 저장
//     → 관리자 화면에 "테스트 신청 · 국세청 확인 안 함"으로 보이므로, 승인할지는 관리자가 판단
@Service
@Transactional(readOnly = true)
public class OwnerApplicationService {

    // 같은 사업자번호를 다른 회원이 쓰고 있으면 막는 상태 (반려된 신청은 제외)
    private static final List<OwnerApplicationStatus> ACTIVE_STATUSES =
            List.of(OwnerApplicationStatus.PENDING, OwnerApplicationStatus.APPROVED);

    // 개업일자 "오늘" 기준 (서버가 UTC 로 돌아도 한국 날짜로 비교)
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final OwnerApplicationRepository ownerApplicationRepository;
    private final MemberService memberService; // 조회(findById)만 사용, 수정 없음
    private final NtsBusinessApiClient ntsBusinessApiClient;
    private final TransactionTemplate transactionTemplate; // apply() 의 저장 부분만 트랜잭션으로 묶을 때 사용

    public OwnerApplicationService(OwnerApplicationRepository ownerApplicationRepository,
                                   MemberService memberService,
                                   NtsBusinessApiClient ntsBusinessApiClient,
                                   PlatformTransactionManager transactionManager) {
        this.ownerApplicationRepository = ownerApplicationRepository;
        this.memberService = memberService;
        this.ntsBusinessApiClient = ntsBusinessApiClient;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // ── 사장 신청 (일반 회원) ──────────────────────────────────
    // NOT_SUPPORTED: 이 메서드 전체를 트랜잭션으로 묶지 않음
    //   국세청 API 호출(최대 십여 초)을 하는 동안 회원 행 잠금 · DB 연결을 붙잡고 있지 않도록
    //   ① 미리 확인(잠금 없음) → ② 국세청 확인 → ③ 잠금 걸고 다시 확인 + 저장(transactionTemplate)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public OwnerApplication apply(Long memberId, OwnerApplicationForm form) {
        // 번호는 숫자만 저장 (하이픈 제거). 형식은 OwnerApplicationForm 의 @Pattern 이 이미 확인함
        //   → 중복 신청 검사도 "1234567891" 하나의 모양으로 비교됨
        String businessNumber = digitsOnly(form.getBusinessNumber());
        String phone = digitsOnly(form.getPhone());
        String address = busanAddress(form.getAddress()); // 부산 주소가 아니면 400 (국세청 호출 전에 확인)

        // 1단계: 형식 · 체크섬
        if (!isValidBusinessNumber(businessNumber)) {
            throw new IllegalArgumentException("존재할 수 없는 사업자등록번호입니다. 번호를 다시 확인해주세요.");
        }
        // 개업일자: 없는 날짜(2020-02-30) · 미래 날짜면 국세청을 부르기 전에 안내
        //   (그대로 보내면 "국세청 등록 정보와 일치하지 않습니다"로만 나와서 날짜가 틀린 줄 모름)
        checkOpenDate(form.getOpenDate());

        // 테스트 신청(시연용): 국세청 API 대신 테스트 번호표로 결과를 정함, 사업자번호 중복 검사도 하지 않음
        boolean test = form.isTestMode();

        // ① 신청할 수 없는 상황이면 국세청 API 를 부르기 전에 끝냄 (역할은 ③에서 잠금 걸고 확인)
        checkNoActiveApplication(memberId, businessNumber, test);

        // 2단계: 국세청 진위확인 + 상태 (실패 시 UnavailableException → 503)
        NtsBusinessApiClient.Result nts = test
                ? ntsBusinessApiClient.verifyForTest(businessNumber)
                : ntsBusinessApiClient.verify(businessNumber, form.getOwnerName().trim(),
                                              form.getOpenDate().replace("-", ""));
        checkNtsResult(nts);

        // ③ 저장: 국세청 확인하는 사이 같은 회원의 다른 요청이 먼저 신청했을 수 있으므로 잠금 후 다시 확인
        return transactionTemplate.execute(status -> {
            // 세션의 loginMember 는 로그인 시점 정보라 역할이 오래됐을 수 있음 → DB 에서 다시 읽음 (+ 잠금)
            Member member = ownerApplicationRepository.findMemberForUpdate(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
            if (member.getRole() != Role.USER) {
                throw new IllegalStateException("일반 회원만 사장 권한을 신청할 수 있습니다.");
            }
            checkNoActiveApplication(memberId, businessNumber, test);

            OwnerApplication application = OwnerApplication.builder()
                    .member(member)
                    .storeName(form.getStoreName().trim())
                    .address(address)
                    .phone(phone)
                    .businessNumber(businessNumber)
                    .message(form.getMessage() == null || form.getMessage().isBlank()
                            ? null : form.getMessage().trim())
                    .build();
            if (test) {
                application.markTestApplication(nts.statusName()); // 국세청 확인 기록 없이 "테스트 신청"으로 구분
            } else {
                application.markBusinessVerified(nts.statusName(), nts.taxType());
            }
            return ownerApplicationRepository.save(application);
        });
    }

    // 내 신청이 심사 중이거나, 같은 사업자번호를 다른 회원이 실제 신청에 쓰고 있으면 신청 불가
    //   테스트 신청은 사업자번호 중복을 검사하지 않음 (시연 계정끼리 같은 테스트 번호를 써도 됨)
    private void checkNoActiveApplication(Long memberId, String businessNumber, boolean test) {
        if (ownerApplicationRepository.existsByMemberIdAndStatus(memberId, OwnerApplicationStatus.PENDING)) {
            throw new IllegalStateException("이미 심사 중인 신청이 있습니다.");
        }
        if (!test && ownerApplicationRepository.existsByBusinessNumberAndStatusInAndMemberIdNotAndTestApplicationFalse(
                businessNumber, ACTIVE_STATUSES, memberId)) {
            throw new IllegalArgumentException("이미 다른 회원이 이 사업자등록번호로 신청했거나 승인받았습니다. "
                    + "본인 사업자번호가 맞다면 관리자에게 문의해주세요.");
        }
    }

    // 개업일자 검사 (형식 yyyy-MM-dd 는 OwnerApplicationForm 의 @Pattern 이 이미 확인함)
    //   LocalDate.parse: ISO 형식은 엄격하게 읽어서 2020-02-30, 2020-13-01 같은 없는 날짜면 DateTimeParseException
    //   화면(apply.html)의 max=오늘 은 화면을 거치지 않은 요청을 막지 못하므로 서버에서 한 번 더
    static void checkOpenDate(String openDate) {
        LocalDate date;
        try {
            date = LocalDate.parse(openDate);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("개업일자가 올바른 날짜가 아닙니다. 사업자등록증의 개업연월일을 확인해주세요.");
        }
        if (date.isAfter(LocalDate.now(SEOUL))) {
            throw new IllegalArgumentException("개업일자는 오늘 이후 날짜일 수 없습니다.");
        }
    }

    // 국세청 결과: 정보 불일치 · 휴업 · 폐업이면 신청 불가 (400)
    private void checkNtsResult(NtsBusinessApiClient.Result nts) {
        if (!nts.matched()) {
            throw new IllegalArgumentException("국세청 등록 정보와 일치하지 않습니다. "
                    + "사업자등록번호 · 대표자 성명 · 개업일자를 사업자등록증과 똑같이 입력해주세요.");
        }
        switch (nts.statusCode()) {
            case "01" -> { /* 계속사업자: 통과 */ }
            case "02" -> throw new IllegalArgumentException("휴업 중인 사업자는 신청할 수 없습니다.");
            case "03" -> throw new IllegalArgumentException("폐업한 사업자는 신청할 수 없습니다.");
            default -> throw new IllegalArgumentException(
                    "영업 중인 사업자만 신청할 수 있습니다. (국세청 상태: " + nts.statusName() + ")");
        }
    }

    // 가게 주소 검사: "부산광역시 …"로 통일하고, 부산이 아닌 주소는 막음
    //   RestaurantService.validAddress()(맛집 등록)와 같은 규칙. 그 메서드는 private 이라 같은 규칙을 여기에 둠
    //   화면(apply.html 의 applyAddress)에서도 막지만, 화면을 거치지 않은 요청도 막으려고 서버에서 한 번 더
    private String busanAddress(String address) {
        String a = (address == null) ? "" : address.trim().replaceAll("\\s+", " ");
        if (a.startsWith("부산광역시 ")) {
            return a;
        }
        if (a.startsWith("부산시 ")) {
            return "부산광역시 " + a.substring("부산시 ".length());
        }
        if (a.startsWith("부산 ")) {
            return "부산광역시 " + a.substring("부산 ".length());
        }
        throw new IllegalArgumentException("부산 지역 가게만 신청할 수 있습니다. [주소 검색]으로 부산 주소를 선택해주세요.");
    }

    // 번호에서 숫자만 남김: "123-45-67891" → "1234567891", "010-1234-5678" → "01012345678"
    static String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    // 1단계: 사업자등록번호 검증 숫자(마지막 자리) 확인
    //   앞 9자리에 가중치 1,3,7,1,3,7,1,3,5 를 곱해 더하고, 9번째 자리 × 5 의 십의 자리를 한 번 더 더함
    //   (10 - 합계 % 10) % 10 이 마지막 자리와 같아야 함
    static boolean isValidBusinessNumber(String businessNumber) {
        String d = businessNumber == null ? "" : businessNumber.replace("-", "");
        if (!d.matches("\\d{10}")) {
            return false;
        }
        int[] weights = {1, 3, 7, 1, 3, 7, 1, 3, 5};
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (d.charAt(i) - '0') * weights[i];
        }
        sum += ((d.charAt(8) - '0') * 5) / 10;
        return (10 - sum % 10) % 10 == d.charAt(9) - '0';
    }

    // ── 내 가장 최근 신청 (신청 페이지 상태 표시용) ─────────────
    public Optional<OwnerApplication> findMyLatest(Long memberId) {
        return ownerApplicationRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId);
    }

    // ── 지금 DB 에 저장된 내 역할 ───────────────────────────────
    // 세션의 loginMember.role 은 로그인 시점 값이라, 승인 후나 관리자가 역할을 바꾼 뒤에는 실제와 다를 수 있음.
    // 신청 페이지는 이 값으로 "신청 가능 / 이미 사장 / 다시 로그인 필요"를 판단함
    public Role findCurrentRole(Long memberId) {
        return memberService.findById(memberId).getRole();
    }

    // ── 관리자: 상태별 목록 ─────────────────────────────────────
    public Page<OwnerApplication> findByStatus(OwnerApplicationStatus status, Pageable pageable) {
        return ownerApplicationRepository.findByStatus(status, pageable);
    }

    // ── 관리자: 승인 ────────────────────────────────────────────
    // 신청 상태 변경과 회원 역할 변경을 한 트랜잭션에서 처리 → 둘 중 하나만 반영되는 일이 없음
    @Transactional
    public void approve(Long applicationId) {
        OwnerApplication application = ownerApplicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("신청을 찾을 수 없습니다."));

        application.approve(); // PENDING 이 아니면 IllegalStateException

        // 심사 도중 관리자가 회원 관리에서 역할을 직접 바꿨을 수도 있으므로
        // USER 일 때만 OWNER 로 올림 (ADMIN 을 OWNER 로 내리는 일 방지)
        Member member = application.getMember();
        if (member.getRole() == Role.USER) {
            member.changeRole(Role.OWNER);
        }
    }

    // ── 관리자: 반려 ────────────────────────────────────────────
    @Transactional
    public void reject(Long applicationId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("반려 사유를 입력해주세요.");
        }
        String trimmed = reason.trim();
        if (trimmed.length() > 500) {
            throw new IllegalArgumentException("반려 사유는 500자 이내로 입력해주세요.");
        }

        OwnerApplication application = ownerApplicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("신청을 찾을 수 없습니다."));

        application.reject(trimmed); // PENDING 이 아니면 IllegalStateException
    }
}
