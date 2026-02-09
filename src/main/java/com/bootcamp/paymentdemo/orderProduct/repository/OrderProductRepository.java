package com.bootcamp.paymentdemo.orderProduct.repository;

import com.bootcamp.paymentdemo.orderProduct.entity.OrderProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderProductRepository extends JpaRepository<OrderProduct, Long> {


    //주문 ID로 주문 상품 목록 조회
    List<OrderProduct> findByOrderId(Long orderId);
}
