package com.example.mymatzip.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

// 쿠폰 발급 JSON { title, discount, expiresAt: "2026-12-31" }
public record CouponForm(@NotBlank @Size(max = 50) String title,
                         @Min(100) @Max(1000000) int discount,
                         @NotNull @FutureOrPresent(message = "기한은 오늘 이후로 정해 주세요.") LocalDate expiresAt) {}