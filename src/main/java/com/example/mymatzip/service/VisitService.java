package com.example.mymatzip.service;

import com.example.crm.dto.VisitForm;
import com.example.crm.entity.*;
import com.example.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// ============================================================
// [VisitService] 방문 등록 · 포인트 (C 방문·포인트 담당)
// ============================================================
// 방문 1건 등록 = visit INSERT + (포인트 사용 시) point_history USE + (결제액 × 적립률) point_history EARN
// @Transactional 하나로 묶어서 중간에 실패하면 모두 되돌린다 (JpaTransactionManager 객체가 rollback)
// ============================================================
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VisitService {

    private final VisitRepository visitRepository;
    private final PointHistoryRepository pointHistoryRepository;

    public long balance(Long customerId) {
        return pointHistoryRepository.balance(customerId);
    }

    public List<Visit> recentVisits(Long customerId) {
        return visitRepository.findTop20ByCustomerIdOrderByVisitedAtDesc(customerId);
    }

    public List<PointHistory> recentPoints(Long customerId) {
        return pointHistoryRepository.findTop20ByCustomerIdOrderByCreatedAtDescIdDesc(customerId);
    }

    public long visitCount(Long customerId) {
        return visitRepository.countByCustomerId(customerId);
    }

    // 호출: CustomerApiController.addVisit() → 반환: 적립된 포인트
    @Transactional
    public int register(Customer customer, VisitForm form) {
        long balance = pointHistoryRepository.balance(customer.getId());
        if (form.usePoint() > balance) throw new IllegalArgumentException("포인트 잔액(" + balance + "P)보다 많이 쓸 수 없습니다.");
        if (form.usePoint() > form.amount()) throw new IllegalArgumentException("결제 금액보다 많은 포인트는 쓸 수 없습니다.");

        LocalDateTime now = LocalDateTime.now();
        Visit visit = visitRepository.save(new Visit(customer, now, form.amount(), form.usePoint(), form.menuMemo()));
        if (form.usePoint() > 0) {
            pointHistoryRepository.save(new PointHistory(customer, visit, PointType.USE, form.usePoint(), now));
        }
        // 포인트는 실제 낸 금액(결제액 - 사용 포인트) 기준으로 적립
        int earn = (form.amount() - form.usePoint()) * customer.getStore().getPointRate() / 100;
        if (earn > 0) {
            pointHistoryRepository.save(new PointHistory(customer, visit, PointType.EARN, earn, now));
        }
        return earn;
    }
}