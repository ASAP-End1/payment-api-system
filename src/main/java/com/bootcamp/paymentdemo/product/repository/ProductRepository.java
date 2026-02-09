package com.bootcamp.paymentdemo.product.repository;

import com.bootcamp.paymentdemo.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product,Long> {
}
