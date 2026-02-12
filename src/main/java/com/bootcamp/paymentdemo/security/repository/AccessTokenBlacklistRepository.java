package com.bootcamp.paymentdemo.security.repository;

import com.bootcamp.paymentdemo.security.entity.AccessTokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AccessTokenBlacklistRepository extends JpaRepository<AccessTokenBlacklist, Long> {
    // 토큰이 블랙리스트에 있는지 확인
    boolean existsByToken(String token);

    // 블랙리스트에서 만료된 토큰 삭제(스케줄러 사용)
    @Modifying
    @Query("DELETE FROM AccessTokenBlacklist a WHERE a.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}
