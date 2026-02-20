package com.bootcamp.paymentdemo.orderProduct.repository;

import com.bootcamp.paymentdemo.orderProduct.entity.OrderProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderProductRepository extends JpaRepository<OrderProduct, Long> {



    List<OrderProduct> findByOrder_Id(Long orderId);
}
