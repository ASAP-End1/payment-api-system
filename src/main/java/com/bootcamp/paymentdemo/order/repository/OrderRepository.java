package com.bootcamp.paymentdemo.order.repository;

import com.bootcamp.paymentdemo.order.consts.OrderStatus;
import com.bootcamp.paymentdemo.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order,Long> {
    List<Order> findByOrderStatusAndCreatedAtBefore(OrderStatus orderStatus, LocalDateTime date);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    Optional<Order> findByOrderNumber(String orderNumber);

    // 특정 사용자의 모든 주문 조회
    List<Order> findByUser_UserIdOrderByCreatedAtDesc(Long userId);
}
