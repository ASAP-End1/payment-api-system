package com.bootcamp.paymentdemo.order.service;

import com.bootcamp.paymentdemo.membership.service.MembershipService;
import com.bootcamp.paymentdemo.order.consts.OrderStatus;
import com.bootcamp.paymentdemo.order.dto.*;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.exception.OrderNotFoundException;
import com.bootcamp.paymentdemo.order.repository.OrderRepository;
import com.bootcamp.paymentdemo.orderProduct.entity.OrderProduct;
import com.bootcamp.paymentdemo.orderProduct.repository.OrderProductRepository;
import com.bootcamp.paymentdemo.point.service.PointService;
import com.bootcamp.paymentdemo.product.entity.Product;
import com.bootcamp.paymentdemo.product.exception.ProductNotFoundException;
import com.bootcamp.paymentdemo.product.repository.ProductRepository;
import com.bootcamp.paymentdemo.product.service.ProductService;
import com.bootcamp.paymentdemo.user.entity.User;
import com.bootcamp.paymentdemo.user.exception.UserNotFoundException;
import com.bootcamp.paymentdemo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderProductRepository orderProductRepository;
    private final UserRepository userRepository;
    private final PointService pointService;
    private final ProductService productService;
    private final MembershipService membershipService;

    @Transactional
    public OrderCreateResponse createOrder(OrderCreateRequest request, String email) {


        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        log.info("주문 생성 시작: userId={}, email={}", user.getUserId(), email);


        BigDecimal totalAmount = BigDecimal.ZERO;
        List<TempProductInfo> tempItems = new ArrayList<>();

        for (var itemRequest : request.getOrderItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException("상품을 찾을 수 없습니다."));

            tempItems.add(new TempProductInfo(product, itemRequest.getCount()));
            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getCount())));
        }


        BigDecimal usedPoints = (request.getUsedPoints() != null) ? request.getUsedPoints() : BigDecimal.ZERO;


        validatePointsToUse(user, usedPoints);


        BigDecimal finalAmount = totalAmount.subtract(usedPoints);

        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("최종 결제 금액은 0보다 작을 수 없습니다.");
        }


        BigDecimal earnedPoints = calculateEarnedPoints(user, finalAmount);


        Order order = orderRepository.save(Order.builder()
                .user(user)
                .orderNumber(generateOrderNumber())
                .totalAmount(totalAmount)
                .usedPoints(usedPoints)
                .finalAmount(finalAmount)
                .earnedPoints(earnedPoints)
                .orderStatus(OrderStatus.PENDING_PAYMENT)
                .currency("KRW")
                .build());

        log.info("주문 생성 완료: orderId={}, orderNumber={}, totalAmount={}, usedPoints={}, finalAmount={}, earnedPoints={}",
                order.getId(), order.getOrderNumber(), totalAmount, usedPoints, finalAmount, earnedPoints);


        List<OrderProduct> orderProducts = tempItems.stream()
                .map(item -> OrderProduct.builder()
                        .order(order)
                        .productId(item.product.getId())
                        .productName(item.product.getName())
                        .orderPrice(item.product.getPrice())
                        .count(item.count)
                        .build())
                .toList();

        orderProductRepository.saveAll(orderProducts);


        for (OrderProduct orderProduct : orderProducts) {
            productService.decreaseStock(orderProduct.getProductId(), orderProduct.getCount());
        }

        log.info("재고 차감 완료: orderId={}, 상품 수={}", order.getId(), orderProducts.size());



        return OrderCreateResponse.from(order);
    }


    @Transactional(readOnly = true)
    public List<OrderGetDetailResponse> findAllOrders(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));


        List<Order> orders = orderRepository.findByUser_UserIdOrderByCreatedAtDesc(user.getUserId());

        List<OrderGetDetailResponse> orderGetResponses = orders.stream()
                .map(order -> OrderGetDetailResponse.from(order, new ArrayList<>()))
                .toList();
        return orderGetResponses;
    }


    @Transactional(readOnly = true)
    public OrderGetDetailResponse findOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다."));

        List<OrderProduct> orderProducts = orderProductRepository.findByOrder_Id(orderId);


        List<OrderProductGetResponse> orderProductGetResponses = orderProducts.stream()
                .map(op -> new OrderProductGetResponse(
                        op.getProductId(),
                        op.getProductName(),
                        op.getOrderPrice(),
                        op.getCount()
                ))
                .toList();

        log.info("주문 상세 조회: orderId={}, 주문상품 수={}", orderId, orderProductGetResponses.size());

        return OrderGetDetailResponse.from(order, orderProductGetResponses);
    }


    @Transactional
    public void completePayment(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다."));


        order.completePayment();

        log.info("결제 완료 처리: orderId={}, orderNumber={}, 현재 상태={}",
                orderId, order.getOrderNumber(), order.getOrderStatus());


        User user = order.getUser();

        if (order.getUsedPoints().compareTo(BigDecimal.ZERO) > 0) {
            pointService.usePoints(user, order);
        }
    }


    @Transactional
    public void confirmOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다."));


        order.confirm();

        log.info("주문 수동 확정 완료: orderId={}, orderNumber={}, 현재 상태={}",
                orderId, order.getOrderNumber(), order.getOrderStatus());


        User user = order.getUser();
        pointService.earnPoints(user, order);
        log.info("포인트 적립 완료: userId={}, orderId={}, 적립 포인트={}",
                user.getUserId(), orderId, order.getEarnedPoints());


        membershipService.handleOrderCompleted(
                order.getUser().getUserId(),
                order.getFinalAmount(),
                order.getId()
        );
    }


    @Transactional
    public void cancelOrder(Long orderId, String cancelReason) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다."));


        order.cancel();

        log.info("주문 취소 완료: orderId={}, orderNumber={}, 현재 상태={}, 취소 사유={}",
                orderId, order.getOrderNumber(), order.getOrderStatus(), cancelReason);


        User user = order.getUser();

        if (order.getUsedPoints().compareTo(BigDecimal.ZERO) > 0) {
            pointService.refundPoints(user, order);
            log.info("사용 포인트 복구 완료: userId={}, orderId={}, 복구 포인트={}",
                    user.getUserId(), orderId, order.getUsedPoints());
        }
    }



    private String generateOrderNumber() {

        LocalDate today = LocalDate.now();
        String datePart = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));


        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        long todayOrderCount = orderRepository.countByCreatedAtBetween(startOfDay, endOfDay);


        long nextSeq = todayOrderCount + 1;



        return String.format("ORD-%s-%04d", datePart, nextSeq);
    }


    private void validatePointsToUse(User user, BigDecimal pointsToUse) {

        if (pointsToUse == null || pointsToUse.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }


        BigDecimal currentBalance = pointService.checkPointBalance(user);




        if (pointsToUse.compareTo(currentBalance) > 0) {
            throw new IllegalArgumentException(
                    String.format("포인트 잔액이 부족합니다. (보유: %s, 사용 요청: %s)",
                            currentBalance, pointsToUse)
            );
        }

        log.info("포인트 잔액 검증 완료: userId={}, 보유={}, 사용={}",
                user.getUserId(), currentBalance, pointsToUse);
    }



    private BigDecimal calculateEarnedPoints(User user, BigDecimal finalAmount) {

        BigDecimal earnedPoints = finalAmount
                .multiply(user.getCurrentGrade().getAccRate())
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);

        log.info("적립 예정 포인트 계산: userId={}, finalAmount={}, accRate={}%, earnedPoints={}",
                user.getUserId(), finalAmount, user.getCurrentGrade().getAccRate(), earnedPoints);

        return earnedPoints;
    }



    private record TempProductInfo(Product product, int count) {
    }

    @Transactional
    public void rollbackUsedPoint(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다."));

        if (order.getUsedPoints().compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        order.cancelPointUsage();
    }
}