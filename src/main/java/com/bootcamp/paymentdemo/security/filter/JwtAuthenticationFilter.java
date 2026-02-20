package com.bootcamp.paymentdemo.security.filter;

import com.bootcamp.paymentdemo.security.provider.JwtTokenProvider;
import com.bootcamp.paymentdemo.security.entity.RefreshToken;
import com.bootcamp.paymentdemo.security.repository.AccessTokenBlacklistRepository;
import com.bootcamp.paymentdemo.security.repository.RefreshTokenRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ObjectMapper objectMapper;
    private final AccessTokenBlacklistRepository blacklistRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, RefreshTokenRepository refreshTokenRepository,ObjectMapper objectMapper, AccessTokenBlacklistRepository blacklistRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.objectMapper = objectMapper;
        this.blacklistRepository = blacklistRepository;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        try {

            String token = getJwtFromRequest(request);

            if (token != null) {
                try {

                    if (blacklistRepository.existsByToken(token)) {
                        log.warn("블랙리스트에 있는 토큰 사용 시도");
                        sendErrorResponse(response, 401, "TOKEN_BLACKLISTED", "Token has been revoked");
                        return;
                    }


                    jwtTokenProvider.validateToken(token);


                    String tokenType = jwtTokenProvider.getTokenType(token);

                    if ("access".equals(tokenType)) {

                        processAuthentication(token, request);

                    } else if ("refresh".equals(tokenType)) {

                        log.warn("Refresh Token으로 API 접근 시도: uri={}", request.getRequestURI());
                        sendErrorResponse(
                                response,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "REFRESH_TOKEN_NOT_ALLOWED",
                                "Refresh Token cannot be used for API authentication"
                        );
                        return;
                    }
                } catch (ExpiredJwtException e) {

                    log.info("Access Token 만료 감지 - 갱신 시도: uri={}", request.getRequestURI());

                    String newAccessToken = tryRefreshToken(request);

                    if (newAccessToken != null) {

                        processAuthentication(newAccessToken, request);


                        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken);

                        log.info("Access Token 갱신 성공: uri={}", request.getRequestURI());

                    } else {

                        log.warn("Refresh Token 갱신 실패 - 로그인 필요: uri={}", request.getRequestURI());
                        sendErrorResponse(
                                response,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "TOKEN_EXPIRED",
                                "Both Access Token and Refresh Token expired. Please login again."
                        );
                        return;
                    }
                }
            }

        }  catch (MalformedJwtException e) {

            log.warn("잘못된 형식의 JWT 토큰: uri={}, message={}", request.getRequestURI(), e.getMessage());
            sendErrorResponse(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "INVALID_TOKEN_FORMAT",
                    "Invalid JWT token format"
            );
            return;

        } catch (SignatureException e) {

            log.warn("JWT 서명 검증 실패: uri={}, message={}", request.getRequestURI(), e.getMessage());
            sendErrorResponse(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "INVALID_TOKEN_SIGNATURE",
                    "Invalid JWT token signature"
            );
            return;

        } catch (Exception e) {

            log.error("JWT 인증 처리 중 예외 발생: uri={}", request.getRequestURI(), e);
            sendErrorResponse(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "AUTHENTICATION_ERROR",
                    "An error occurred during authentication"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }





    private String tryRefreshToken(HttpServletRequest request) {
        try {

            String refreshToken = getRefreshTokenFromCookie(request);
            if (refreshToken == null) {
                log.warn("Refresh Token 쿠키 없음");
                return null;
            }


            jwtTokenProvider.validateToken(refreshToken);

            String tokenType = jwtTokenProvider.getTokenType(refreshToken);
            if (!"refresh".equals(tokenType)) {
                log.warn("Refresh Token 타입이 아님");
                return null;
            }


            RefreshToken storedToken = refreshTokenRepository
                    .findByToken(refreshToken)
                    .orElse(null);

            if (storedToken == null) {
                log.warn("DB에 Refresh Token 없음");
                return null;
            }


            if (storedToken.getRevoked()) {
                log.warn("무효화된 Refresh Token");
                return null;
            }


            if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                log.warn("만료된 Refresh Token");
                return null;
            }


            String email = jwtTokenProvider.getEmail(refreshToken);
            if (email == null) {
                log.warn("Refresh Token에서 이메일 추출 실패");
                return null;
            }


            String newAccessToken = jwtTokenProvider.createAccessToken(email);

            log.info("새 Access Token 발급 성공: email={}", email);

            return newAccessToken;

        } catch (ExpiredJwtException e) {
            log.warn("Refresh Token 만료");
            return null;
        } catch (Exception e) {
            log.error("Refresh Token 처리 중 오류", e);
            return null;
        }
    }


    private void processAuthentication(String token, HttpServletRequest request) {
        String email = jwtTokenProvider.getEmail(token);

        if (email != null) {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                    );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("JWT 인증 성공: email={}", email);
        }
    }


    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }


    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> "refreshToken".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }


    private void sendErrorResponse(
            HttpServletResponse response,
            int status,
            String errorCode,
            String message
    ) throws IOException {

        if (response.isCommitted()) {
            log.warn("응답이 이미 commit되어 에러 응답을 보낼 수 없습니다");
            return;
        }

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", errorCode);
        errorResponse.put("message", message);
        errorResponse.put("status", status);
        errorResponse.put("timestamp", System.currentTimeMillis());

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        response.getWriter().flush();
        response.flushBuffer();
    }

}
