package com.bootcamp.paymentdemo.security.scheduler;

import com.bootcamp.paymentdemo.security.repository.AccessTokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final AccessTokenBlacklistRepository blacklistRepository;


    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupExpiredTokens() {
        blacklistRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("만료된 블랙리스트 토큰 삭제 완료");
    }

}
