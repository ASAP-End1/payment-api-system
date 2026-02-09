package com.bootcamp.paymentdemo.product.controller;

import com.bootcamp.paymentdemo.product.dto.ProductGetResponse;
import com.bootcamp.paymentdemo.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    // 상품 전체 조회
    @GetMapping
    public ResponseEntity<List<ProductGetResponse>> getAllProducts()
    {
        log.info("상품 목록 조회"); // 요청 시작 로그

        List<ProductGetResponse> products = productService.findAllProducts();

        log.info("상품 목록 조회 완료 - 개수: {}개", products.size()); // 결과 로그

        if(products.isEmpty()){
            return ResponseEntity.noContent().build(); // 비어있을 경우 204 코드 반환, 공통 응답 처리 의논 필요
        }
        return ResponseEntity.ok(products);
    }

    // 상품 단건 조회 (productId 경로 변수로 전달받은 상품)
    @GetMapping("/{productId}")
    public ResponseEntity<ProductGetResponse> getProductById(@PathVariable Long productId)
    {
        log.info("상품 단건 조회 - ID: {}", productId);
        ProductGetResponse response = productService.findOneProduct(productId);
        return ResponseEntity.ok(response);
    }
}
