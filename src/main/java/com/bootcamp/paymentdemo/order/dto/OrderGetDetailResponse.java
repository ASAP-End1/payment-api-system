package com.bootcamp.paymentdemo.order.dto;

import com.bootcamp.paymentdemo.order.entity.Order;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class OrderGetDetailResponse {


    private final Long orderId;
    private final String orderNumber;
    private final String status;
    private final LocalDateTime createdAt;


    private final BigDecimal totalAmount;
    private final BigDecimal usedPoints;
    private final BigDecimal finalAmount;
    private final BigDecimal earnedPoints;


    private final List<OrderProductGetResponse> orderProducts;


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


    @Getter
    public static class PointSummary {
        private final BigDecimal used;
        private final BigDecimal earned;
        private final BigDecimal net;

        public PointSummary(BigDecimal used, BigDecimal earned) {
            this.used = used != null ? used : BigDecimal.ZERO;
            this.earned = earned != null ? earned : BigDecimal.ZERO;
            this.net = this.earned.subtract(this.used);
        }
    }
}
