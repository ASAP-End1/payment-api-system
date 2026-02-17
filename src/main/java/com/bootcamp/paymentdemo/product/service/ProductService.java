package com.bootcamp.paymentdemo.product.service;

import com.bootcamp.paymentdemo.product.dto.ProductGetResponse;
import com.bootcamp.paymentdemo.product.entity.Product;
import com.bootcamp.paymentdemo.product.exception.ProductNotFoundException;
import com.bootcamp.paymentdemo.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    // 상품 목록 조회
    @Transactional(readOnly = true)
    public List<ProductGetResponse> findAllProducts() {
        List<ProductGetResponse> products =  productRepository.findAll().stream()
                .map(ProductGetResponse::from)
                .toList();

        log.info("상품 목록 조회 완료: 개수={}", products.size());

        return products;
    }

    // 상품 단건 조회
    @Transactional(readOnly = true)
    public ProductGetResponse findOneProduct(Long productId) {
        ProductGetResponse product = productRepository.findById(productId)
                .map(ProductGetResponse::from)
                .orElseThrow(() -> new ProductNotFoundException("상품을 찾을 수 없습니다."));

        log.info("상품 단건 조회 완료: productId={}", productId);

        return product;
    }

    // 재고 차감 (주문 생성 시 호출)
    @Transactional
    public void decreaseStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("상품을 찾을 수 없습니다."));

        product.decreaseStock(quantity);

        log.info("재고 차감 완료: productId={}, productName={}, 차감수량={}, 남은재고={}",
                productId, product.getName(), quantity, product.getStock());
    }

    // 재고 복구 (주문 취소/환불 시 호출)
    @Transactional
    public void increaseStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("상품을 찾을 수 없습니다."));

        product.increaseStock(quantity);

        log.info("재고 복구 완료: productId={}, productName={}, 복구수량={}, 현재재고={}",
                productId, product.getName(), quantity, product.getStock());
    }
}
