package com.bootcamp.paymentdemo.security.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "access_token_blacklists")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccessTokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long tokenId;

    @Column(nullable = false, unique = true)
    private String token;  // 무효화된 access token

    @Column(nullable = false)
    private String email;  // 토큰 소유자

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt; // 토큰 만료 시간

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;  // 블랙리스트 추가 시간

    public static AccessTokenBlacklist create(String accessToken, String email, LocalDateTime expiresAt) {
        AccessTokenBlacklist blacklist = new AccessTokenBlacklist();
        blacklist.token = accessToken;
        blacklist.email = email;
        blacklist.expiresAt = expiresAt;
        blacklist.createdAt = LocalDateTime.now();
        return blacklist;
    }

}
