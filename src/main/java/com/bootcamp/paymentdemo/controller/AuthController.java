package com.bootcamp.paymentdemo.controller;

import com.bootcamp.paymentdemo.security.JwtTokenProvider;
import com.bootcamp.paymentdemo.user.dto.*;
import com.bootcamp.paymentdemo.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * 인증 관련 API 컨트롤러
 * client-api-config.yml의 API 계약을 따름
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthController {

    //private final AuthenticationManager authenticationManager;
    //private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

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
     *   "message": null
     * }
     */
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        log.info("회원가입 요청: email={}", request.getEmail());
        SignupResponse response = userService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
     *   "email": "user@example.com"
     * }
     */

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("로그인 요청: email={}", request.getEmail());

        UserService.TokenPair tokenPair = userService.login(request);

        LoginResponse response = LoginResponse.success(tokenPair.email);

        // Authorization 헤더에 Access Token 포함
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPair.accessToken);

        // Refresh Token은 별도 쿠키나 응답 헤더로 전달 가능
        headers.set("X-Refresh-Token", tokenPair.refreshToken);

        return ResponseEntity.status(HttpStatus.OK).headers(headers).body(response);
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
     *   "message": "로그아웃되었습니다."
     * }
     */
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(Principal principal) {
        log.info("로그아웃 요청: email={}", principal.getName());

        // LogoutResponse 반환
        LogoutResponse response = userService.logout(principal.getName());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    /**
     * 현재 로그인한 사용자 정보 조회 API
     * GET /api/me
     *
     * 응답:
     * {
     *   "success": true,
     *   "email": "user@example.com",
     *   "customerUid": "CUST_xxxxx",
     *   "name": "홍길동"
     * }
     *
     * 중요: customerUid는 PortOne 빌링키 발급 시 활용!
     */
    @GetMapping("/me")
    public ResponseEntity<UserSearchResponse> getCurrentUser(Principal principal) {
        log.info("현재 사용자 조회: email={}", principal.getName());

        UserSearchResponse response = userService.getCurrentUser(principal.getName());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
