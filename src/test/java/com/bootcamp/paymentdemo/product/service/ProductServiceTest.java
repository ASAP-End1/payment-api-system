package com.bootcamp.paymentdemo.product.service;

import com.bootcamp.paymentdemo.product.consts.ProductStatus;
import com.bootcamp.paymentdemo.product.dto.ProductGetResponse;
import com.bootcamp.paymentdemo.product.entity.Product;
import com.bootcamp.paymentdemo.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    private Product testProduct1;
    private Product testProduct2;

    @BeforeEach
    void setUp() {
        testProduct1 = new Product(
                "테스트 상품 1",
                new BigDecimal("10000"),
                100,
                "테스트 카테고리",
                ProductStatus.FOR_SALE
        );
        testProduct1 = productRepository.save(testProduct1);

        testProduct2 = new Product(
                "테스트 상품 2",
                new BigDecimal("20000"),
                50,
                "테스트 카테고리",
                ProductStatus.FOR_SALE
        );
        testProduct2 = productRepository.save(testProduct2);
    }

    @Test
    @DisplayName("상품 목록 조회 성공")
    void findAllProducts_Success() {
        // when
        List<ProductGetResponse> responses = productService.findAllProducts();

        // then
        assertThat(responses).isNotEmpty();
        assertThat(responses).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("상품 단건 조회 성공")
    void findOneProduct_Success() {
        // when
        ProductGetResponse response = productService.findOneProduct(testProduct1.getId());

        // then
        assertThat(response.getId()).isEqualTo(testProduct1.getId());
        assertThat(response.getName()).isEqualTo("테스트 상품 1");
        assertThat(response.getPrice()).isEqualTo(new BigDecimal("10000"));
        assertThat(response.getStock()).isEqualTo(100);
        assertThat(response.getCategory()).isEqualTo("테스트 카테고리");
        assertThat(response.getStatus()).isEqualTo("FOR_SALE");
    }

    @Test
    @DisplayName("상품 단건 조회 실패 - 존재하지 않는 상품")
    void findOneProduct_NotFound() {
        // when & then
        assertThrows(EntityNotFoundException.class, () -> {
            productService.findOneProduct(999999L);
        });
    }

    @Test
    @DisplayName("재고 차감 성공")
    void decreaseStock_Success() {
        // when
        productService.decreaseStock(testProduct1.getId(), 30);

        // then
        Product updatedProduct = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(70);
    }

    @Test
    @DisplayName("재고 차감 실패 - 재고 부족")
    void decreaseStock_InsufficientStock() {
        // when & then
        assertThrows(IllegalStateException.class, () -> {
            productService.decreaseStock(testProduct1.getId(), 200);
        });

        // 재고가 변경되지 않았는지 확인
        Product unchangedProduct = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(unchangedProduct.getStock()).isEqualTo(100);
    }

    @Test
    @DisplayName("재고 차감 실패 - 잘못된 수량 (0 이하)")
    void decreaseStock_InvalidQuantity() {
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            productService.decreaseStock(testProduct1.getId(), 0);
        });

        // 재고가 변경되지 않았는지 확인
        Product unchangedProduct = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(unchangedProduct.getStock()).isEqualTo(100);
    }

    @Test
    @DisplayName("재고 차감 실패 - 음수 수량")
    void decreaseStock_NegativeQuantity() {
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            productService.decreaseStock(testProduct1.getId(), -10);
        });

        // 재고가 변경되지 않았는지 확인
        Product unchangedProduct = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(unchangedProduct.getStock()).isEqualTo(100);
    }

    @Test
    @DisplayName("재고 차감 실패 - 존재하지 않는 상품")
    void decreaseStock_ProductNotFound() {
        // when & then
        assertThrows(EntityNotFoundException.class, () -> {
            productService.decreaseStock(999999L, 10);
        });
    }

    @Test
    @DisplayName("재고 복구 성공")
    void increaseStock_Success() {
        // when
        productService.increaseStock(testProduct1.getId(), 20);

        // then
        Product updatedProduct = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(120);
    }

    @Test
    @DisplayName("재고 복구 실패 - 잘못된 수량 (0 이하)")
    void increaseStock_InvalidQuantity() {
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            productService.increaseStock(testProduct1.getId(), 0);
        });

        // 재고가 변경되지 않았는지 확인
        Product unchangedProduct = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(unchangedProduct.getStock()).isEqualTo(100);
    }

    @Test
    @DisplayName("재고 복구 실패 - 음수 수량")
    void increaseStock_NegativeQuantity() {
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            productService.increaseStock(testProduct1.getId(), -5);
        });

        // 재고가 변경되지 않았는지 확인
        Product unchangedProduct = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(unchangedProduct.getStock()).isEqualTo(100);
    }

    @Test
    @DisplayName("재고 복구 실패 - 존재하지 않는 상품")
    void increaseStock_ProductNotFound() {
        // when & then
        assertThrows(EntityNotFoundException.class, () -> {
            productService.increaseStock(999999L, 10);
        });
    }

    @Test
    @DisplayName("재고 차감 후 복구 - 원상복구 확인")
    void decreaseAndIncreaseStock() {
        // given
        int originalStock = testProduct1.getStock();

        // when - 재고 차감
        productService.decreaseStock(testProduct1.getId(), 30);

        // then - 차감 확인
        Product afterDecrease = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(afterDecrease.getStock()).isEqualTo(70);

        // when - 재고 복구
        productService.increaseStock(testProduct1.getId(), 30);

        // then - 원상복구 확인
        Product afterIncrease = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(afterIncrease.getStock()).isEqualTo(originalStock);
    }
}
