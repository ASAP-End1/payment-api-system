package com.bootcamp.paymentdemo.point.repository;

import com.bootcamp.paymentdemo.point.entity.PointUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointUsageRepository extends JpaRepository<PointUsage, Long> {
}
