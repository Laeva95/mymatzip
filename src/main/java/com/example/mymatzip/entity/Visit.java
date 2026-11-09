package com.example.mymatzip.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

// [Visit] 맛집 방문·결제 기록 (C 담당) → visit 테이블
// ※ 지금은 식당 이름을 문자열로 저장. 팀 통합 때 Restaurant 엔티티가 확정되면
//   restaurantName 대신 @ManyToOne Restaurant 로 바꾸면 된다.
@Entity
@Table(indexes = @Index(name = "idx_visit_user_time", columnList = "user_id, visited_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String restaurantName;

    @Column(nullable = false)
    private LocalDateTime visitedAt;

    @Column(nullable = false)
    private int amount;       // 결제 금액(원)

    @Column(nullable = false)
    private int usedPoint;    // 이번 결제에 쓴 포인트

    @Column(length = 200)
    private String memo;

    public Visit(User user, String restaurantName, LocalDateTime visitedAt,
                 int amount, int usedPoint, String memo) {
        this.user = user;
        this.restaurantName = restaurantName;
        this.visitedAt = visitedAt;
        this.amount = amount;
        this.usedPoint = usedPoint;
        this.memo = memo;
    }
}
