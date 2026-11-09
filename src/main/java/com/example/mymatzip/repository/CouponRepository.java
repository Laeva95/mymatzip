package com.example.mymatzip.repository;

import com.example.mymatzip.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    List<Coupon> findByUserIdOrderByIdDesc(Long userId);
}
