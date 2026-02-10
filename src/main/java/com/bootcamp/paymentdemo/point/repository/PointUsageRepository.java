package com.bootcamp.paymentdemo.point.repository;

import com.bootcamp.paymentdemo.point.entity.PointUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PointUsageRepository extends JpaRepository<PointUsage, Long> {
    @Query("SELECT pu FROM PointUsage pu JOIN FETCH pu.pointTransaction WHERE pu.order.id = :orderId")
    List<PointUsage> findByOrderId(@Param("orderId") Long orderId);
}