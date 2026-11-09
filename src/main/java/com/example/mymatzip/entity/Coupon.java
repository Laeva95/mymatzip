package com.example.mymatzip.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalDateTime;

// [Coupon] 쿠폰 (C 방문·포인트 담당) → coupon 테이블
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false)
    private int discount;          // 할인 금액(원)

    @Column(nullable = false)
    private LocalDate expiresAt;

    private LocalDateTime usedAt;  // null 이면 아직 안 씀

    public Coupon(Customer customer, String title, int discount, LocalDate expiresAt) {
        this.customer = customer;
        this.title = title;
        this.discount = discount;
        this.expiresAt = expiresAt;
    }

    // 호출: CouponService.use(). 이미 썼거나 기한이 지났으면 예외 → 400
    public void use() {
        if (usedAt != null) throw new IllegalArgumentException("이미 사용한 쿠폰입니다.");
        if (expiresAt.isBefore(LocalDate.now())) throw new IllegalArgumentException("기한이 지난 쿠폰입니다.");
        this.usedAt = LocalDateTime.now();
    }

    // 화면 표시용 상태 (타임리프 ${cp.status})
    public String getStatus() {
        if (usedAt != null) return "사용함";
        return expiresAt.isBefore(LocalDate.now()) ? "기한 지남" : "사용 가능";
    }
}