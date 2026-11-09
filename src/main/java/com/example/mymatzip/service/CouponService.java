package com.example.mymatzip.service;

import com.example.crm.dto.CouponForm;
import com.example.crm.entity.Coupon;
import com.example.crm.entity.Customer;
import com.example.crm.entity.Store;
import com.example.crm.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// [CouponService] 쿠폰 발급·사용 (C 방문·포인트 담당)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;

    public List<Coupon> list(Long customerId) {
        return couponRepository.findByCustomerIdOrderByIdDesc(customerId);
    }

    @Transactional
    public Long issue(Customer customer, CouponForm form) {
        return couponRepository.save(new Coupon(customer, form.title().trim(), form.discount(), form.expiresAt())).getId();
    }

    // 다른 매장 쿠폰을 쓰지 못하도록 쿠폰의 고객 → 매장이 로그인 회원의 매장인지 확인
    @Transactional
    public void use(Store store, Long couponId) {
        Coupon c = couponRepository.findById(couponId).orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));
        if (!c.getCustomer().getStore().getId().equals(store.getId())) throw new IllegalArgumentException("내 매장 쿠폰만 사용할 수 있습니다.");
        c.use();   // usedAt 값 변경 → 변경 감지로 UPDATE
    }
}