package com.example.mymatzip.service;

import com.example.mymatzip.dto.OwnerApplicationForm;
import com.example.mymatzip.entity.Member;
import com.example.mymatzip.entity.OwnerApplication;
import com.example.mymatzip.entity.OwnerApplicationStatus;
import com.example.mymatzip.entity.Role;
import com.example.mymatzip.repository.OwnerApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// ============================================================
// [OwnerApplicationService.java] 식당 사장 권한 신청 · 심사
// ============================================================
//
// 예외 규칙 (컨트롤러가 HTTP 상태로 바꿔서 응답)
//   IllegalArgumentException → 대상 없음 / 입력 오류       → 400 또는 404
//   IllegalStateException    → 지금 상태에서 할 수 없는 요청 → 409
//     예: 이미 심사 중인데 또 신청, 이미 OWNER 인데 신청, 이미 처리된 신청을 다시 승인
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OwnerApplicationService {

    private final OwnerApplicationRepository ownerApplicationRepository;
    private final MemberService memberService; // 조회(findById)만 사용, 수정 없음

    // ── 사장 신청 (일반 회원) ──────────────────────────────────
    @Transactional
    public OwnerApplication apply(Long memberId, OwnerApplicationForm form) {
        // 세션의 loginMember 는 로그인 시점 정보라 역할이 오래됐을 수 있음 → DB 에서 다시 읽음 (+ 잠금)
        Member member = ownerApplicationRepository.findMemberForUpdate(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        if (member.getRole() != Role.USER) {
            throw new IllegalStateException("일반 회원만 사장 권한을 신청할 수 있습니다.");
        }
        if (ownerApplicationRepository.existsByMemberIdAndStatus(memberId, OwnerApplicationStatus.PENDING)) {
            throw new IllegalStateException("이미 심사 중인 신청이 있습니다.");
        }

        OwnerApplication application = OwnerApplication.builder()
                .member(member)
                .storeName(form.getStoreName().trim())
                .address(form.getAddress().trim())
                .phone(form.getPhone().trim())
                .businessNumber(form.getBusinessNumber().trim())
                .message(form.getMessage() == null || form.getMessage().isBlank()
                        ? null : form.getMessage().trim())
                .build();
        return ownerApplicationRepository.save(application);
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
