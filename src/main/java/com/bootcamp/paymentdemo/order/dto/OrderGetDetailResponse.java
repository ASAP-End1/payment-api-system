package com.bootcamp.paymentdemo.order.dto;

import com.bootcamp.paymentdemo.order.entity.Order;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class OrderGetDetailResponse {

    // 주문 기본 정보
    private final Long orderId;
    private final String orderNumber;
    private final String status;
    private final LocalDateTime createdAt;

    // 금액 정보
    private final BigDecimal totalAmount;      // 포인트 차감 전 총 금액
    private final BigDecimal usedPoints;       // 사용된 포인트
    private final BigDecimal finalAmount;      // 포인트 차감 후 최종 결제 금액
    private final BigDecimal earnedPoints;     // 적립될 포인트

    // 주문 상품 목록
    private final List<OrderProductGetResponse> orderProducts;

    // 포인트 요약 (간단한 요약 정보)
    private final PointSummary pointSummary;

    public OrderGetDetailResponse(Long orderId, String orderNumber, String status,
                                  LocalDateTime createdAt, BigDecimal totalAmount,
                                  BigDecimal usedPoints, BigDecimal finalAmount,
                                  BigDecimal earnedPoints, List<OrderProductGetResponse> orderProducts) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.status = status;
        this.createdAt = createdAt;
        this.totalAmount = totalAmount;
        this.usedPoints = usedPoints;
        this.finalAmount = finalAmount;
        this.earnedPoints = earnedPoints;
        this.orderProducts = orderProducts;

        // 포인트 요약 자동 생성
        this.pointSummary = new PointSummary(usedPoints, earnedPoints);
    }

    public static OrderGetDetailResponse from(Order order, List<OrderProductGetResponse> orderProducts) {
        return new OrderGetDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getOrderStatus().name(),
                order.getCreatedAt(),
                order.getTotalAmount(),
                order.getUsedPoints(),
                order.getFinalAmount(),
                order.getEarnedPoints(),
                orderProducts
        );
    }

    //포인트 요약 정보
    @Getter
    public static class PointSummary {
        private final BigDecimal used;    // 사용한 포인트
        private final BigDecimal earned;  // 적립 예정 포인트
        private final BigDecimal net;     // 순 포인트 (적립 - 사용)

        public PointSummary(BigDecimal used, BigDecimal earned) {
            this.used = used != null ? used : BigDecimal.ZERO;
            this.earned = earned != null ? earned : BigDecimal.ZERO;
            this.net = this.earned.subtract(this.used);
        }
    }
}
