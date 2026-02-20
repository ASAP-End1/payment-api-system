package com.bootcamp.paymentdemo.security.repository;

import com.bootcamp.paymentdemo.security.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {


    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.userId = :userId")
    void revokeAllByUserId(@Param("userId") Long userId);


    @Query("SELECT rt FROM RefreshToken rt WHERE rt.token = :token AND rt.revoked = false")
    Optional<RefreshToken> findByToken(@Param("token") String token);

}
