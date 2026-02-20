package com.bootcamp.paymentdemo.security.repository;

import com.bootcamp.paymentdemo.security.entity.AccessTokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AccessTokenBlacklistRepository extends JpaRepository<AccessTokenBlacklist, Long> {

    boolean existsByToken(String token);


    @Modifying
    @Query("DELETE FROM AccessTokenBlacklist a WHERE a.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}
