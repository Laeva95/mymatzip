package com.example.mymatzip.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

// [PointHistory] 포인트 적립·사용 내역 (C 담당) → point_history 테이블
// 잔액은 따로 저장하지 않고 SUM(적립) - SUM(사용) 으로 계산한다 (PointHistoryRepository.balance)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 어떤 방문에서 생긴 포인트인지 (방문이 지워지면 이 칸만 NULL)
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "visit_id")
    private Visit visit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PointType type;

    @Column(nullable = false)
    private int point;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public PointHistory(User user, Visit visit, PointType type, int point, LocalDateTime createdAt) {
        this.user = user;
        this.visit = visit;
        this.type = type;
        this.point = point;
        this.createdAt = createdAt;
    }
}
