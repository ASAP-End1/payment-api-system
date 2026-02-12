package com.bootcamp.paymentdemo.security;

import com.bootcamp.paymentdemo.common.dto.ApiResponse;
import com.bootcamp.paymentdemo.user.dto.*;
import com.bootcamp.paymentdemo.user.exception.InvalidCredentialsException;
import com.bootcamp.paymentdemo.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * 인증 관련 API 컨트롤러
 * client-api-config.yml의 API 계약을 따름
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    /**
     * 회원가입 API
     * POST /api/signup
     *
     * Request Body:
     * {
     *   "name": "홍길동",
     *   "email": "user@example.com",
     *   "password": "password123",
     *   "phone": "010-1234-5678"
     * }
     *
     * Response Body:
     * {
     *   "success": true,
     *   "code": "CREATED",
     *   "message": "회원가입 성공",
     *   "data": {
     *       "userId": 1,
     *       "email": "user@example.com"
     *   }
     * }
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        log.info("회원가입 요청: email={}", request.getEmail());
        SignupResponse response = userService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "회원가입 성공", response));
    }


    /**
     * 로그인 API
     * POST /api/login
     *
     * 요청 본문:
     * {
     *   "email": "user@example.com",
     *   "password": "password123"
     * }
     *
     * 응답 헤더:
     * Authorization: Bearer eyJhbGc...
     *
     * 응답 본문:
     * {
     *   "success": true,
     *   "code": "OK",
     *   "message": "로그인 성공",
     *   "data": {
     *       "email": "user@example.com"
     *   }
     * }
     */

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("로그인 요청: email={}", request.getEmail());

        try {
            // Spring Security - AuthenticationManager 사용
            // 내부적으로 CustomUserDetailsService.loadUserByUsername() 호출됨
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // 인증 성공 시 토큰 생성
            String email = authentication.getName();

            // UserService에 토큰 생성 위임
            UserService.TokenPair tokenPair = userService.login(email);

            LoginResponse response = new LoginResponse(tokenPair.email, tokenPair.accessToken);

            // Authorization 헤더에 Access Token 포함
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPair.accessToken);

            // Refresh Token은 HttpOnly 쿠키로 저장
            ResponseCookie refreshCookie = ResponseCookie
                    .from("refreshToken", tokenPair.refreshToken)
                    .httpOnly(true)      // JavaScript 접근 불가 (XSS 방지)
                    .secure(false)      // HTTPS만 (프로덕션: true, 개발: false)
                    .path("/")          // 모든 경로에서 전송
                    .maxAge(7 * 24 * 60 * 60)    // 7일
                    .sameSite("Lax")   // CSRF 방지
                    .build();

            return ResponseEntity.status(HttpStatus.OK)
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .headers(headers)
                    .body(ApiResponse.success(HttpStatus.OK, "로그인 성공", response));

        } catch (AuthenticationException e) {
            // 인증 실패
            log.warn("로그인 실패: email={}", request.getEmail());
            throw new InvalidCredentialsException(
                    "이메일 또는 비밀번호가 올바르지 않습니다"
            );
        }
    }

    /**
     * 로그아웃 API
     * POST /api/logout
     *
     * Headers:
     * Authorization: Bearer {accessToken}
     *
     * Response Body:
     * {
     *   "success": true,
     *   "code": "OK",
     *   "message": "로그아웃되었습니다."
     *   "data": null
     * }
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(Principal principal) {
        log.info("로그아웃 요청: email={}", principal.getName());

        userService.logout(principal.getName());

        // Refresh Token 쿠키 삭제
        ResponseCookie deleteCookie = ResponseCookie
                .from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)  // 즉시 삭제
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(ApiResponse.success(HttpStatus.OK, "로그아웃되었습니다.", null));
    }


    /**
     * 현재 로그인한 사용자 정보 조회 API
     * GET /api/me
     *
     * 응답:
     * {
     *   "success": true,
     *   "code": "OK",
     *   "message": "사용자 조회 성공",
     *   "data": {
     *       "email": "user@example.com",
     *       "customerUid": "CUST_xxxxx",
     *       "name": "홍길동"
     *   }
     * }
     *
     * 중요: customerUid는 PortOne 빌링키 발급 시 활용!
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserSearchResponse>> getCurrentUser(Principal principal) {
        log.info("현재 사용자 조회: email={}", principal.getName());

        UserSearchResponse response = userService.getCurrentUser(principal.getName());

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK, "사용자 조회 성공", response));
    }
}
