package com.bootcamp.paymentdemo.order.service;

import com.bootcamp.paymentdemo.order.consts.OrderStatus;
import com.bootcamp.paymentdemo.order.dto.OrderCreateRequest;
import com.bootcamp.paymentdemo.order.dto.OrderCreateResponse;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.repository.OrderRepository;
import com.bootcamp.paymentdemo.orderProduct.entity.OrderProduct;
import com.bootcamp.paymentdemo.orderProduct.repository.OrderProductRepository;
import com.bootcamp.paymentdemo.point.service.PointService;
import com.bootcamp.paymentdemo.product.entity.Product;
import com.bootcamp.paymentdemo.product.repository.ProductRepository;
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

    @Transactional
    public OrderCreateResponse createOrder(OrderCreateRequest request) {

        // 1. 사용자 조회
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

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
                .userId(String.valueOf(request.getUserId()))
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
                        .orderId(order.getId())
                        .productId(item.product.getId())
                        .productName(item.product.getName())
                        .orderPrice(item.product.getPrice())
                        .count(item.count)
                        .build())
                .toList();

        orderProductRepository.saveAll(orderProducts);

        // 8. 응답 반환 (모든 포인트 정보 포함)
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
    private record TempProductInfo(Product product, int count) {}
}
