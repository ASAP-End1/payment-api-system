package com.bootcamp.paymentdemo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증 유틸리티
 * 개선할 부분: Refresh Token, Token Expiry 관리, Claims 커스터마이징 등
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;

    public JwtTokenProvider(
        @Value("${jwt.secret:commercehub-secret-key-for-demo-please-change-this-in-production-environment}") String secret,
        @Value("${jwt.token-validity-in-seconds:86400}") long tokenValidityInSeconds
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityInMilliseconds = tokenValidityInSeconds * 1000;
        this.refreshTokenValidityInMilliseconds = tokenValidityInSeconds * 1000 * 7; // 7배 (7일)
    }

    /**
     * JWT 토큰 생성
     *
     * TODO: 개선 사항
     * - 사용자 역할(Role) 정보 추가
     * - 추가 Claims 정보 (이름, 이메일 등)
     * - Refresh Token 발급 로직
     */

    // Access Token 생성
    public String createToken(String email) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);

        return Jwts.builder()
            .subject(email)
            .claim("type", "access")
            .issuedAt(now)
            .expiration(validity)
            .signWith(secretKey)
            .compact();
    }

    // Refresh Token 생성
    public String createRefreshToken(String email) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

        return Jwts.builder()
            .subject(email)
            .claim("type", "refresh")
            .issuedAt(now)
            .expiration(validity)
            .signWith(secretKey)
            .compact();
    }

    // Refresh Token의 만료 시간 반환
    public LocalDateTime getRefreshTokenExpiryDate() {
        Date expiryDate = new Date(System.currentTimeMillis() + refreshTokenValidityInMilliseconds);
        return expiryDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }


    /**
     * JWT 토큰에서 사용자 이름 추출
     */
    public String getEmail(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getSubject();
        } catch (Exception e) {
            log.error("토큰에서 이메일 추출 실패", e);
            return null;
        }
    }

    /**
     * JWT 토큰 유효성 검증
     *
     * TODO: 개선 사항
     * - 토큰 블랙리스트 체크 (로그아웃된 토큰)
     * - 토큰 갱신 로직
     * - 상세한 예외 처리
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 토큰: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("잘못된 형식의 토큰: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.warn("서명 검증 실패: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("토큰 검증 중 예외 발생", e);
            return false;
        }
    }

    // 토큰 타입 확인 (access/refresh)
    public String getTokenType(String token) {
        try{
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.get("type", String.class);
        } catch (Exception e) {
            log.error("토큰 타입 확인 실패", e);
            return null;
        }

    }

}
