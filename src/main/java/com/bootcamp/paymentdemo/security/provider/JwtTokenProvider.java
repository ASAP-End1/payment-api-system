package com.bootcamp.paymentdemo.security.provider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;


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
        this.refreshTokenValidityInMilliseconds = tokenValidityInSeconds * 1000 * 7;
    }




    public String createAccessToken(String email) {
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


    public LocalDateTime getRefreshTokenExpiryDate() {
        Date expiryDate = new Date(System.currentTimeMillis() + refreshTokenValidityInMilliseconds);
        return expiryDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }



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


    public void validateToken(String token) {
        Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);

    }


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


    public LocalDateTime getExpirationDate(String accessToken) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();

            Date expiration = claims.getExpiration();
            return expiration.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (ExpiredJwtException e) {

            Date expiration = e.getClaims().getExpiration();
            return expiration.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (Exception e) {
            log.error("토큰에서 만료 시간 추출 실패", e);

            return LocalDateTime.now();
        }
    }
}
