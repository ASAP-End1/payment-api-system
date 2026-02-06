package com.bootcamp.paymentdemo.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
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
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
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

            // 2. 토큰 유효성 검증
            if (token != null && jwtTokenProvider.validateToken(token)) {
                // 3. Access Token인지 확인
                String tokenType = jwtTokenProvider.getTokenType(token);
                if ("access".equals(tokenType)) {
                    // 4. 토큰에서 사용자 정보 추출
                    String email = jwtTokenProvider.getEmail(token);

                    if (email != null) {
                        // 5. 인증 객체 생성
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        email,
                                        null,
                                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                                );

                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        // 6. SecurityContext에 인증 정보 설정
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        log.debug("JWT 인증 성공: email={}, uri={}", email, request.getRequestURI());

                    } else {
                        log.warn("토큰에서 이메일 추출 실패: uri={}", request.getRequestURI());
                    }

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
            } else if (token != null) {
                // 토큰은 있지만 유효하지 않음
                log.warn("유효하지 않은 토큰: {}", request.getRequestURI());
            }

        } catch (ExpiredJwtException e) {
            // 만료된 토큰 처리
            log.warn("만료된 JWT 토큰: uri={}, message={}", request.getRequestURI(), e.getMessage());
            sendErrorResponse(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "TOKEN_EXPIRED",
                    "JWT token has expired. Please login again."
            );
            return;  // 필터 체인 중단

        } catch (MalformedJwtException e) {
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

    /**
     * 에러 응답 전송
     */
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
