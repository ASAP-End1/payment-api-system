package com.bootcamp.paymentdemo.domain.product.service;

import com.bootcamp.paymentdemo.domain.product.dto.ProductGetResponse;
import com.bootcamp.paymentdemo.domain.product.entity.Product;
import com.bootcamp.paymentdemo.domain.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
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
        return productRepository.findAll().stream()
                .map(this::convertToGetProductResponse // 반복 적용되는 로직 별도 메서드로 분리
                ).toList();
    }

    // 상품 단건 조회
    @Transactional(readOnly = true)
    public ProductGetResponse findOneProduct(Long productId) {
        return productRepository.findById(productId)
                .map(this::convertToGetProductResponse
                ).orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다."));
    }

    // 변환 로직 Product 엔티티를 GetProductResponse DTO로 매핑하는 공통 메서드(추후 정적 팩토리 메서드로 리팩토링 가능)
    private ProductGetResponse convertToGetProductResponse(Product product) {
        return new ProductGetResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getCategory(),
                product.getStatus().getStatusName()
        );
    }
}
