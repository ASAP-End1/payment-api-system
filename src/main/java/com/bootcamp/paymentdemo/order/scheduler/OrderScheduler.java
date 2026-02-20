package com.bootcamp.paymentdemo.order.scheduler;

import com.bootcamp.paymentdemo.membership.service.MembershipService;
import com.bootcamp.paymentdemo.order.consts.OrderStatus;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.repository.OrderRepository;
import com.bootcamp.paymentdemo.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderScheduler {

    private final OrderRepository orderRepository;
    private final OrderService orderService;


    @Scheduled(cron = "0 0/30 * * * *")
    public void autoConfirmOrder() {
        log.info("자동 주문 확정 스케줄러 시작");

        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(7);

        List<Order> ordersToConfirm = orderRepository.findByOrderStatusAndCreatedAtBefore(
                OrderStatus.PENDING_CONFIRMATION,
                thresholdDate
        );

        int successCount = 0;
        for (Order order : ordersToConfirm) {
            try {
                orderService.confirmOrder(order.getId());
                successCount++;
            } catch (Exception e) {
                log.error("자동 주문 확정 실패: orderId={}, message={}", order.getId(), e.getMessage());
            }
        }
        log.info("자동 주문 확정 스케줄러 완료: 처리 건수={}", successCount);
    }
}
