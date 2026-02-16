package com.bootcamp.paymentdemo.point.scheduler;

import com.bootcamp.paymentdemo.point.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PointScheduler {

    private final PointService pointService;

    // 포인트 소멸 (매일 00시 실행)
    @Scheduled(cron = "0 0 0 * * *")
    public void autoExpiredPoints() {
        log.info("포인트 소멸 스케줄러 시작");
        int successCount = pointService.expirePoints();
        log.info("포인트 소멸 스케줄러 완료: 처리 건수={}", successCount);
    }

    // 스냅샷 정합성 보정 (매일 00시 30분 실행 - 소멸 후)
    @Scheduled(cron = "0 30 0 * * *")
    public void autoSyncPointBalance() {
        log.info("스냅샷 정합성 보정 스케줄러 시작");
        int successCount = pointService.syncPointBalance();
        log.info("스냅샷 정합성 보정 스케줄러 완료: 처리 건수={}", successCount);
    }
}
