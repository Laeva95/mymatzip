
package com.example.mymatzip.repository;

import com.example.mymatzip.entity.Visit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    List<Visit> findTop20ByUserIdOrderByVisitedAtDesc(Long userId);

    long countByUserId(Long userId);
}