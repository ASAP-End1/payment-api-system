package com.bootcamp.paymentdemo.point.repository;

import com.bootcamp.paymentdemo.point.entity.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointRepository extends JpaRepository<PointTransaction, Long> {
    // TODO 페이징 적용
    List<PointTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}
