package com.example.mymatzip.repository;

import com.example.mymatzip.entity.Member;
import com.example.mymatzip.entity.OwnerApplication;
import com.example.mymatzip.entity.OwnerApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.Optional;

// ============================================================
// [OwnerApplicationRepository.java] 식당 사장 신청 DB 접근
// ============================================================
public interface OwnerApplicationRepository extends JpaRepository<OwnerApplication, Long> {

    // 이미 심사 중인 신청이 있는지 (중복 신청 방지)
    // 예: existsByMemberIdAndStatus(회원ID, PENDING)
    boolean existsByMemberIdAndStatus(Long memberId, OwnerApplicationStatus status);

    // 같은 사업자번호로 "다른 회원"이 심사 중이거나 승인받은 실제 신청이 있는지
    //   (한 사업자로 여러 계정이 사장 권한을 받는 것 방지. 본인이 권한 해제 후 같은 번호로 다시 신청하는 것은 허용)
    //   테스트 신청(testApplication = true)은 세지 않음 → 시연에서 쓴 번호가 실제 신청을 막지 않음
    // 예: existsByBusinessNumberAndStatusInAndMemberIdNotAndTestApplicationFalse("1234567891", List.of(PENDING, APPROVED), 회원ID)
    //   번호는 숫자만 저장되므로 숫자만으로 비교 (OwnerApplicationService.digitsOnly)
    // 사용처 두 곳
    //   신청할 때(apply)  : PENDING · APPROVED 로 검사 — 국세청 확인 전 1번, 저장 직전(회원 행 잠금 후) 1번
    //   승인할 때(approve): APPROVED 만 검사 — 다른 회원 두 명이 동시에 신청해 둘 다 심사 중이 된 경우 둘 다 승인되는 것 방지
    //                      (하이픈이 남은 예전 데이터도 잡으려고 "123-45-67891" 모양으로도 한 번 더 검사)
    boolean existsByBusinessNumberAndStatusInAndMemberIdNotAndTestApplicationFalse(
            String businessNumber, Collection<OwnerApplicationStatus> statuses, Long memberId);

    // 내 가장 최근 신청 1건 (신청 페이지에서 "심사 중 / 반려됨" 상태 표시용)
    Optional<OwnerApplication> findTopByMemberIdOrderByCreatedAtDesc(Long memberId);

    // 관리자 목록: 상태별 페이지 조회. 신청자 닉네임 표시를 위해 member 를 함께 조회 (N+1 방지)
    @EntityGraph(attributePaths = {"member"})
    Page<OwnerApplication> findByStatus(OwnerApplicationStatus status, Pageable pageable);

    // 승인/반려 처리용 조회. 행에 쓰기 잠금(SELECT ... FOR UPDATE)을 걸어
    // 관리자 두 명이 같은 신청을 동시에 승인·반려해도 한 명만 처리되게 함
    // join fetch 로 신청자(member) 행도 함께 잠김 → 승인 중에 같은 회원의 신청 저장(findMemberForUpdate)과도 순서가 정해짐
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from OwnerApplication a join fetch a.member where a.id = :id")
    Optional<OwnerApplication> findByIdForUpdate(@Param("id") Long id);

    // 신청 시 신청자 회원 행에 쓰기 잠금. 같은 회원이 "신청" 버튼을 동시에 두 번 눌러도
    // 두 번째 요청은 첫 번째가 끝날 때까지 기다린 뒤 "이미 심사 중" 검사에 걸림.
    // (MemberRepository 는 다른 팀원 파일이라 수정하지 않고 여기에 둠)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :memberId")
    Optional<Member> findMemberForUpdate(@Param("memberId") Long memberId);
}
