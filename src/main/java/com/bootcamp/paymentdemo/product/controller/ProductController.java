package com.bootcamp.paymentdemo.product.controller;

import com.bootcamp.paymentdemo.common.dto.ApiResponse;
import com.bootcamp.paymentdemo.product.dto.ProductGetResponse;
import com.bootcamp.paymentdemo.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<ApiResponse<List<ProductGetResponse>>> getAllProducts()
    {
        log.info("상품 목록 조회 요청");

        List<ProductGetResponse> products = productService.findAllProducts();

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK, "상품 목록 조회 성공", products));
    }

    // 상품 단건 조회 (productId 경로 변수로 전달받은 상품)
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductGetResponse>> getProductById(@PathVariable Long productId)
    {
        log.info("상품 단건 조회 요청: productId={}", productId);
        ProductGetResponse response = productService.findOneProduct(productId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK, "상품 단건 조회 성공", response));
    }
}
