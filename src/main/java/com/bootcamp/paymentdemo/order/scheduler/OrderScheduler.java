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

    // 시간이 지나면 자동 확정
    @Scheduled(cron = "0 * * * * *") // 매 분 0초 마다 실행
    public void autoConfirmOrder() {
        log.info("자동 주문 확정 스케줄러 실행");

        LocalDateTime thresholdDate = LocalDateTime.now().minusMinutes(1); // 테스트용 1분 이후 확정

        // 현재는 리스트로 가져오는 방식
        List<Order> ordersToConfirm = orderRepository.findByOrderStatusAndCreatedAtBefore(
                OrderStatus.PENDING_CONFIRMATION,
                thresholdDate
        );

        int successCount = 0; // 성공 횟수 담기(로그를 정확하게 표시하기 위해)
        for (Order order : ordersToConfirm) {
            try {
                orderService.confirmOrder(order.getId());
                successCount++;
            } catch (Exception e) {
                log.error("주문 ID {} 자동 확정 실패: {}", order.getId(), e.getMessage());
            }
        }
        log.info("총 {}건의 주문이 자동으로 확정되었습니다.", successCount);
    }
}
