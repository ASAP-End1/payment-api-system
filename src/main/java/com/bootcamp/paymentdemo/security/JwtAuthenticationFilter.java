package com.bootcamp.paymentdemo.security;

import com.bootcamp.paymentdemo.user.entity.RefreshToken;
import com.bootcamp.paymentdemo.user.repository.RefreshTokenRepository;
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

/**
 * JWT 토큰 인증 필터
 * 모든 요청에서 JWT 토큰을 검증하고 SecurityContext에 인증 정보 설정
 *
 * TODO: 개선 사항
 * - 역할(Role) 정보를 토큰에서 추출
 * - 예외 처리 개선
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, RefreshTokenRepository refreshTokenRepository,ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // 1. Request Header에서 JWT 토큰 추출
            String token = getJwtFromRequest(request);

            if (token != null) {
                try {
                    // 2. 토큰 유효성 검증
                    jwtTokenProvider.validateToken(token);

                    // 3. Access Token인지 확인
                    String tokenType = jwtTokenProvider.getTokenType(token);

                    if ("access".equals(tokenType)) {
                        // 정상 Access Token
                        processAuthentication(token, request);

                    } else if ("refresh".equals(tokenType)) {
                        // Refresh Token으로 API 접근 시도
                        log.warn("Refresh Token으로 API 접근 시도: uri={}", request.getRequestURI());
                        sendErrorResponse(
                                response,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "REFRESH_TOKEN_NOT_ALLOWED",
                                "Refresh Token cannot be used for API authentication"
                        );
                        return;  // 필터 체인 중단
                    }
                } catch (ExpiredJwtException e) {
                    // 만료된 토큰 처리 (Access Token 만료 -> Refresh Token으로 갱신)
                    log.info("Access Token 만료 감지 - 갱신 시도: uri={}", request.getRequestURI());

                    String newAccessToken = tryRefreshToken(request);

                    if (newAccessToken != null) {
                        // 갱신 성공
                        processAuthentication(newAccessToken, request);

                        // 새 Access Token을 응답 헤더에 추가
                        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken);

                        log.info("Access Token 갱신 성공: uri={}", request.getRequestURI());

                    } else {
                        // Refresh Token도 없거나 만료
                        log.warn("Refresh Token 갱신 실패 - 로그인 필요: uri={}", request.getRequestURI());
                        sendErrorResponse(
                                response,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "TOKEN_EXPIRED",
                                "Both Access Token and Refresh Token expired. Please login again."
                        );
                        return; // 필터 체인 중단
                    }
                }
            }

        }  catch (MalformedJwtException e) {
            // 잘못된 형식의 토큰
            log.warn("잘못된 형식의 JWT 토큰: uri={}, message={}", request.getRequestURI(), e.getMessage());
            sendErrorResponse(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "INVALID_TOKEN_FORMAT",
                    "Invalid JWT token format"
            );
            return;

        } catch (SignatureException e) {
            // 서명 검증 실패
            log.warn("JWT 서명 검증 실패: uri={}, message={}", request.getRequestURI(), e.getMessage());
            sendErrorResponse(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "INVALID_TOKEN_SIGNATURE",
                    "Invalid JWT token signature"
            );
            return;

        } catch (Exception e) {
            // 기타 예외
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



    // Refresh Token으로 새 Access Token 발급 시도
    // -> return 새 Access Token (실패 시 null)
    private String tryRefreshToken(HttpServletRequest request) {
        try {
            // 1. 쿠키에서 Refresh Token 추출
            String refreshToken = getRefreshTokenFromCookie(request);
            if (refreshToken == null) {
                log.warn("Refresh Token 쿠키 없음");
                return null;
            }

            // 2. Refresh Token 검증
            jwtTokenProvider.validateToken(refreshToken);

            String tokenType = jwtTokenProvider.getTokenType(refreshToken);
            if (!"refresh".equals(tokenType)) {
                log.warn("Refresh Token 타입이 아님");
                return null;
            }

            // 3. DB에서 Refresh Token 조회
            RefreshToken storedToken = refreshTokenRepository
                    .findByToken(refreshToken)
                    .orElse(null);

            if (storedToken == null) {
                log.warn("DB에 Refresh Token 없음");
                return null;
            }

            // 4. 무효화 여부 확인
            if (storedToken.getRevoked()) {
                log.warn("무효화된 Refresh Token");
                return null;
            }

            // 5. 만료 확인
            if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                log.warn("만료된 Refresh Token");
                return null;
            }

            // 6. 이메일 추출 및 새 Access Token 발급
            String email = jwtTokenProvider.getEmail(refreshToken);
            if (email == null) {
                log.warn("Refresh Token에서 이메일 추출 실패");
                return null;
            }

            // 새 Access Token 생성
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

    // 인증 처리 (SecurityContext에 인증 정보 설정)
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

    /**
     * Request Header에서 JWT 토큰 추출
     * Authorization: Bearer {token}
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    // 쿠키에서 Refresh Token 추출
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

    // 에러 응답 전송
    private void sendErrorResponse(
            HttpServletResponse response,
            int status,
            String errorCode,
            String message
    ) throws IOException {
        // 이미 응답이 commit된 경우 무시
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
