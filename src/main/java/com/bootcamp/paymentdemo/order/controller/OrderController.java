package com.bootcamp.paymentdemo.order.controller;

import com.bootcamp.paymentdemo.order.dto.OrderCreateRequest;
import com.bootcamp.paymentdemo.order.dto.OrderCreateResponse;
import com.bootcamp.paymentdemo.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderCreateResponse> createOrder(@RequestBody OrderCreateRequest request)
    {
        log.info("주문 생성 요청 - 사용자: {}", request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }
}
