package com.bootcamp.paymentdemo.orderProduct.repository;

import com.bootcamp.paymentdemo.orderProduct.entity.OrderProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderProductRepository extends JpaRepository<OrderProduct, Long> {
}
