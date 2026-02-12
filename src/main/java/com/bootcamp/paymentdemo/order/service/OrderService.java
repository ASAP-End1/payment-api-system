package com.bootcamp.paymentdemo.order.service;

import com.bootcamp.paymentdemo.membership.service.MembershipService;
import com.bootcamp.paymentdemo.order.consts.OrderStatus;
import com.bootcamp.paymentdemo.order.dto.*;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.repository.OrderRepository;
import com.bootcamp.paymentdemo.orderProduct.entity.OrderProduct;
import com.bootcamp.paymentdemo.orderProduct.repository.OrderProductRepository;
import com.bootcamp.paymentdemo.point.service.PointService;
import com.bootcamp.paymentdemo.product.entity.Product;
import com.bootcamp.paymentdemo.product.repository.ProductRepository;
import com.bootcamp.paymentdemo.product.service.ProductService;
import com.bootcamp.paymentdemo.user.entity.User;
import com.bootcamp.paymentdemo.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
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

        // 1. 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        log.info("주문 생성 시작 - userId: {}, email: {}", user.getUserId(), email);

        // 2. 상품 정보 조회 및 총 금액 계산
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<TempProductInfo> tempItems = new ArrayList<>();

        for (var itemRequest : request.getOrderItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다."));

            tempItems.add(new TempProductInfo(product, itemRequest.getCount()));
            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getCount())));
        }

        // 3. 포인트 계산 및 검증
        BigDecimal usedPoints = (request.getUsedPoints() != null) ? request.getUsedPoints() : BigDecimal.ZERO;

        // 포인트 사용 시 유효성 검사
        validatePointsToUse(user, usedPoints);

        // 4. 최종 결제 금액 계산
        BigDecimal finalAmount = totalAmount.subtract(usedPoints);

        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("최종 결제 금액은 0보다 작을 수 없습니다.");
        }

        // 5. 적립 포인트 계산 (멤버십 등급 기반)
        BigDecimal earnedPoints = calculateEarnedPoints(user, finalAmount);

        // 6. 주문 엔티티 저장
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

        // 7. OrderProduct 저장
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

        // 8. 재고 차감
        for (OrderProduct orderProduct : orderProducts) {
            productService.decreaseStock(orderProduct.getProductId(), orderProduct.getCount());
        }

        log.info("재고 차감 완료: orderId={}, 상품 수={}", order.getId(), orderProducts.size());

        // 9. 응답 반환 (모든 포인트 정보 포함)
        // 포인트는 결제 완료 시점에 차감됨
        return new OrderCreateResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getTotalAmount(),
                order.getUsedPoints(),
                order.getFinalAmount(),
                order.getEarnedPoints(),
                order.getOrderStatus().name()
        );
    }

    // 주문 내역 조회
    @Transactional(readOnly = true)
    public List<OrderGetDetailResponse> findAllOrders(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        // 해당 사용자의 주문만 조회로 수정
        List<Order> orders = orderRepository.findByUser_UserIdOrderByCreatedAtDesc(user.getUserId());

        List<OrderGetDetailResponse> orderGetResponses = new ArrayList<>();
        for (Order order : orders) {
            OrderGetDetailResponse OrderGetDetailResponse = new OrderGetDetailResponse(
                    order.getId(),
                    order.getOrderNumber(),
                    order.getOrderStatus().name(),
                    order.getCreatedAt(),
                    order.getTotalAmount(),
                    order.getUsedPoints(),
                    order.getFinalAmount(),
                    order.getEarnedPoints(),
                    new ArrayList<>()
            );
            orderGetResponses.add(OrderGetDetailResponse);
        }
        return orderGetResponses;
    }

    // 주문 상세 조회
    @Transactional(readOnly = true)
    public OrderGetDetailResponse findOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다."));

        List<OrderProduct> orderProducts = orderProductRepository.findByOrder_Id(orderId);

        // OrderProduct 변환
        List<OrderProductGetResponse> orderProductGetResponses = orderProducts.stream()
                .map(op -> new OrderProductGetResponse(
                        op.getProductId(),
                        op.getProductName(),
                        op.getOrderPrice(),
                        op.getCount()
                ))
                .toList();

        log.info("주문 상세 조회: orderId={}, 주문상품 수={}", orderId, orderProductGetResponses.size());

        return new OrderGetDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getOrderStatus().name(),
                order.getCreatedAt(),
                order.getTotalAmount(),
                order.getUsedPoints(),
                order.getFinalAmount(),
                order.getEarnedPoints(),
                orderProductGetResponses
        );
    }

    // 결제 완료 처리 Payment 서비스에서만 호출
    @Transactional
    public void completePayment(Long orderId) {
        // 1. 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다."));

        // 2. 결제 완료 처리 (상태 검증은 엔티티 내부에서 처리)
        order.completePayment();

        log.info("결제 완료 처리: orderId={}, orderNumber={}, 현재 상태={}",
                orderId, order.getOrderNumber(), order.getOrderStatus());

        // 3. 포인트 사용 (차감)
        User user = order.getUser();

        if (order.getUsedPoints().compareTo(BigDecimal.ZERO) > 0) {
            pointService.usePoints(user, order);
        }
    }

    // 주문 수동 확정
    @Transactional
    public void confirmOrder(Long orderId) {
        // 1. 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다."));

        // 2. 주문 확정 (상태 검증은 엔티티 내부에서 처리)
        order.confirm();

        log.info("주문 수동 확정 완료: orderId={}, orderNumber={}, 현재 상태={}",
                orderId, order.getOrderNumber(), order.getOrderStatus());

        // 3. 포인트 적립
        User user = order.getUser();
        pointService.earnPoints(user, order);
        log.info("포인트 적립 완료: userId={}, orderId={}, 적립 포인트={}",
                user.getUserId(), orderId, order.getEarnedPoints());

        // 4. 멤버십 갱신
        membershipService.handleOrderCompleted(
                order.getUser().getUserId(),
                order.getFinalAmount(),
                order.getId()
        );
    }

    // 주문 취소 (환불 시 사용)
    @Transactional
    public void cancelOrder(Long orderId, String cancelReason) {
        // 1. 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다."));

        // 2. 주문 취소 (상태 검증은 엔티티 내부에서 처리)
        order.cancel();

        log.info("주문 취소 완료: orderId={}, orderNumber={}, 현재 상태={}, 취소 사유={}",
                orderId, order.getOrderNumber(), order.getOrderStatus(), cancelReason);

        // 3. 사용한 포인트 복구
        User user = order.getUser();

        if (order.getUsedPoints().compareTo(BigDecimal.ZERO) > 0) {
            pointService.refundPoints(user, order);
            log.info("사용 포인트 복구 완료: userId={}, orderId={}, 복구 포인트={}",
                    user.getUserId(), orderId, order.getUsedPoints());
        }
    }

    // ----------
    // 주문 번호 생성
    private String generateOrderNumber() {
        // 1. 오늘 날짜 (yyyyMMdd)
        LocalDate today = LocalDate.now();
        String datePart = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 2. 오늘 생성된 주문 수 조회
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        long todayOrderCount = orderRepository.countByCreatedAtBetween(startOfDay, endOfDay);

        // 3. 순번 생성 (기존 개수 + 1)
        long nextSeq = todayOrderCount + 1;

        // 4. 포맷팅 (ORD-날짜-4자리숫자)
        // %04d : 숫자가 4자리보다 작으면 앞에 0을 채움 (1 -> 0001, 12 -> 0012)
        return String.format("ORD-%s-%04d", datePart, nextSeq);
    }

    // 포인트 사용 가능 여부 검증
    private void validatePointsToUse(User user, BigDecimal pointsToUse) {
        // 포인트 사용이 없으면 검증 불필요
        if (pointsToUse == null || pointsToUse.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        // 현재 포인트 잔액 조회
        BigDecimal currentBalance = pointService.checkPointBalance(user);

        // 잔액 부족 시 예외 발생
        // BigDecimal 비교: compareTo() 사용
        // compareTo 반환값: 0 (같음), 1 (pointsToUse가 더 큼), -1 (pointsToUse가 더 작음)
        if (pointsToUse.compareTo(currentBalance) > 0) {
            throw new IllegalArgumentException(
                    String.format("포인트 잔액이 부족합니다. (보유: %s, 사용 요청: %s)",
                            currentBalance, pointsToUse)
            );
        }

        log.info("포인트 잔액 검증 완료: userId={}, 보유={}, 사용={}",
                user.getUserId(), currentBalance, pointsToUse);
    }


    // 적립 예정 포인트 계산 (멤버십 등급 기반)
    private BigDecimal calculateEarnedPoints(User user, BigDecimal finalAmount) {
        // 사용자의 멤버십 등급에 따른 적립률 적용
        BigDecimal earnedPoints = finalAmount
                .multiply(user.getCurrentGrade().getAccRate())
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);

        log.info("적립 예정 포인트 계산: userId={}, finalAmount={}, accRate={}%, earnedPoints={}",
                user.getUserId(), finalAmount, user.getCurrentGrade().getAccRate(), earnedPoints);

        return earnedPoints;
    }


    // 임시 저장
    private record TempProductInfo(Product product, int count) {
    }

    @Transactional
    public void rollbackUsedPoint(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다."));

        if (order.getUsedPoints().compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        order.cancelPointUsage();
    }
}