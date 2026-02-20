package com.bootcamp.paymentdemo.security.entity;

import com.bootcamp.paymentdemo.common.entity.Base;
import com.bootcamp.paymentdemo.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends Base {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long tokenId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked", nullable = false)
    private Boolean revoked = false;


    public static RefreshToken createToken(User user, String token, LocalDateTime expiresAt) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.user = user;
        refreshToken.token = token;
        refreshToken.expiresAt = expiresAt;
        refreshToken.revoked = false;
        return refreshToken;
    }


    public void revoke() {
        this.revoked = true;
    }


    public boolean isExpired() {
        return this.expiresAt.isBefore(LocalDateTime.now());
    }



    public boolean isValid() {
        return !this.revoked && !isExpired();
    }

}