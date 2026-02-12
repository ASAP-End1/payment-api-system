package com.bootcamp.paymentdemo.order.controller;

import com.bootcamp.paymentdemo.common.dto.ApiResponse;
import com.bootcamp.paymentdemo.order.dto.OrderCreateRequest;
import com.bootcamp.paymentdemo.order.dto.OrderCreateResponse;
import com.bootcamp.paymentdemo.order.dto.OrderGetDetailResponse;
import com.bootcamp.paymentdemo.order.dto.OrderGetResponse;
import com.bootcamp.paymentdemo.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderCreateResponse>> createOrder(@RequestBody OrderCreateRequest request, Principal principal)
    {
        String email = principal.getName();
        log.info("주문 생성 요청 - 사용자: {}", email);
        OrderCreateResponse response = orderService.createOrder(request, email);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "주문 생성 성공", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderGetDetailResponse>>> getAllOrders(Principal principal){
        String email = principal.getName();
        log.info("주문 목록 조회 요청");
        List<OrderGetDetailResponse> response = orderService.findAllOrders(email);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK, "주문 목록 조회 성공", response));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderGetDetailResponse>> getOntOrder(@PathVariable("orderId") Long orderId){
        log.info("주문 상세 조회 요청 - 주문번호: {}", orderId);
        OrderGetDetailResponse response = orderService.findOrderDetail(orderId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK, "주문 상세 조회 성공", response));
    }

    @PatchMapping("/{orderId}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmOrder(@PathVariable("orderId") Long orderId){
        log.info("주문 수동 확정 요청 - 주문번호: {}", orderId);
        orderService.confirmOrder(orderId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK, "주문 확정 성공", null));
    }
}
