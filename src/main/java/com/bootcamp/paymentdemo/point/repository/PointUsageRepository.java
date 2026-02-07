package com.bootcamp.paymentdemo.point.repository;

import com.bootcamp.paymentdemo.point.entity.PointUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointUsageRepository extends JpaRepository<PointUsage, Long> {
    List<PointUsage> findByOrderId(Long id);
}