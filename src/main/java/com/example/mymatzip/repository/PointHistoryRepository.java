package com.example.mymatzip.repository;

import com.example.mymatzip.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    List<PointHistory> findTop20ByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    // 잔액 = 적립 합계 - 사용 합계. 기록이 없으면 0
    @Query("SELECT COALESCE(SUM(CASE WHEN p.type = com.example.mymatzip.entity.PointType.EARN "
            + "THEN p.point ELSE -p.point END), 0) FROM PointHistory p WHERE p.user.id = :userId")
    long balance(@Param("userId") Long userId);
}