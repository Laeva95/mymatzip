package com.example.mymatzip.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

// ============================================================
// [OwnerApplication.java] 식당 사장 권한 신청 엔티티 -- DB 의 owner_application 테이블
// ============================================================
//
// 흐름: 일반 회원(USER)이 신청 → status = PENDING
//       관리자가 승인 → approve()  → status = APPROVED, (서비스에서) member.changeRole(OWNER)
//       관리자가 반려 → reject(사유) → status = REJECTED, rejectReason 저장
//
// 한 회원이 여러 번 신청할 수 있습니다. (반려 후 재신청)
// 단, PENDING 상태 신청은 한 번에 하나만 있어야 하며 서비스에서 검사합니다.
//
// DB 테이블 구조:
//   | id | member_id | store_name | address | phone | business_number | message
//   | status | reject_reason | created_at | reviewed_at |
//   | business_status | tax_type | business_verified_at |   ← 국세청 확인 결과 (신청할 때 저장)
//   | test_application |                                     ← 시연용 테스트 신청 여부
//
// 대표자 성명 · 개업일자는 국세청 확인에만 쓰고 저장하지 않습니다. (개인정보 최소 저장)
// 이 기능 전에 들어온 신청은 business_verified_at 이 null (관리자 화면에 "확인 전 신청"으로 표시)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OwnerApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 신청한 회원. 회원이 삭제되면 신청 기록도 DB 에서 함께 삭제 (Wishlist, Review 와 동일한 방식)
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 100)
    private String storeName;       // 가게 이름

    @Column(nullable = false)
    private String address;         // 가게 주소

    @Column(nullable = false, length = 20)
    private String phone;           // 연락처 (숫자만, 예: 0511234567)

    @Column(nullable = false, length = 12)
    private String businessNumber;  // 사업자등록번호 (숫자만, 예: 1234567891)

    @Column(length = 1000)
    private String message;         // 관리자에게 남기는 말 (선택)

    // varchar 로 고정: 나중에 상태가 추가돼도 DB ENUM 을 ALTER 할 필요가 없음
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private OwnerApplicationStatus status;

    @Column(length = 500)
    private String rejectReason;    // 반려 사유 (REJECTED 일 때만 값이 있음)

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 신청 시각

    private LocalDateTime reviewedAt; // 승인/반려 처리 시각 (PENDING 이면 null)

    @Column(length = 20)
    private String businessStatus;    // 국세청 사업 상태 (예: 계속사업자)

    @Column(length = 100)
    private String taxType;           // 국세청 과세 유형 (예: 부가가치세 일반과세자)

    private LocalDateTime businessVerifiedAt; // 국세청 진위확인 통과 시각 (테스트 신청이면 null)

    // 시연 · 테스트용 신청 (국세청 확인 없이 테스트 번호표로 통과). 관리자 화면에 "테스트 신청"으로 표시
    // default false: 이 컬럼이 생기기 전에 들어온 신청은 실제 신청으로 봄
    // columnDefinition 에는 not null 을 쓰지 않음 (nullable = false 를 보고 Hibernate 가 뒤에 not null 을 붙임)
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean testApplication;

    @Builder
    public OwnerApplication(Member member, String storeName, String address,
                            String phone, String businessNumber, String message) {
        this.member = member;
        this.storeName = storeName;
        this.address = address;
        this.phone = phone;
        this.businessNumber = businessNumber;
        this.message = message;
        this.status = OwnerApplicationStatus.PENDING; // 신청 직후는 항상 심사 중
        this.createdAt = LocalDateTime.now();
    }

    // 국세청 진위확인 통과 결과 기록 (신청 저장 직전에 서비스에서 호출)
    public void markBusinessVerified(String businessStatus, String taxType) {
        this.businessStatus = businessStatus;
        this.taxType = taxType;
        this.businessVerifiedAt = LocalDateTime.now();
    }

    // 테스트 신청 기록: 국세청 확인을 하지 않았으므로 businessVerifiedAt 은 비워 둠
    public void markTestApplication(String businessStatus) {
        this.testApplication = true;
        this.businessStatus = businessStatus;
        this.taxType = null;
        this.businessVerifiedAt = null;
    }

    // 관리자 승인. Role 변경(member.changeRole)은 같은 트랜잭션의 서비스에서 처리
    public void approve() {
        checkPending();
        this.status = OwnerApplicationStatus.APPROVED;
        this.reviewedAt = LocalDateTime.now();
    }

    // 관리자 반려
    public void reject(String reason) {
        checkPending();
        this.status = OwnerApplicationStatus.REJECTED;
        this.rejectReason = reason;
        this.reviewedAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return this.status == OwnerApplicationStatus.PENDING;
    }

    // 이미 승인/반려된 신청을 다시 처리하지 못하게 막음
    private void checkPending() {
        if (!isPending()) {
            throw new IllegalStateException("이미 처리된 신청입니다.");
        }
    }
}
