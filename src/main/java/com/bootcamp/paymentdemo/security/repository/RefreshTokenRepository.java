package com.bootcamp.paymentdemo.security.repository;

import com.bootcamp.paymentdemo.security.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // 사용자 ID로 모든 Refresh Token 무효화
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.userId = :userId")
    void revokeAllByUserId(@Param("userId") Long userId);

    // Refresh Token 문자열로 조회 (무효화되지 않은 것만)
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.token = :token AND rt.revoked = false")
    Optional<RefreshToken> findByToken(@Param("token") String token);

}
