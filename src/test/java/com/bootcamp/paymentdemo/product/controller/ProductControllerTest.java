package com.bootcamp.paymentdemo.product.controller;

import com.bootcamp.paymentdemo.product.consts.ProductStatus;
import com.bootcamp.paymentdemo.product.entity.Product;
import com.bootcamp.paymentdemo.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class ProductControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProductRepository productRepository;

    private Product testProduct1;
    private Product testProduct2;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

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
    @DisplayName("GET /api/products - 상품 목록 조회 성공")
    @WithMockUser
    void getAllProducts_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("상품 목록 조회 성공"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("GET /api/products/{productId} - 상품 단건 조회 성공")
    @WithMockUser
    void getProductById_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/products/{productId}", testProduct1.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("상품 단건 조회 성공"))
                .andExpect(jsonPath("$.data.id").value(testProduct1.getId()))
                .andExpect(jsonPath("$.data.name").value("테스트 상품 1"))
                .andExpect(jsonPath("$.data.price").value(10000))
                .andExpect(jsonPath("$.data.stock").value(100))
                .andExpect(jsonPath("$.data.category").value("테스트 카테고리"))
                .andExpect(jsonPath("$.data.status").value("FOR_SALE"));
    }

    @Test
    @DisplayName("GET /api/products/{productId} - 존재하지 않는 상품 조회 실패")
    @WithMockUser
    void getProductById_NotFound() throws Exception {
        // when & then
        mockMvc.perform(get("/api/products/{productId}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}
