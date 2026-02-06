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

        return ResponseEntity.ok(response);
    }


    /**
     * 현재 로그인한 사용자 정보 조회 API
     * GET /api/auth/me
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
    /*
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Principal principal) {

        String email = principal.getName();

        // TODO: 구현
        // 데이터베이스에서 사용자 정보 조회
        // customerUid 생성은 조회 한 사용자 정보로 조합하여 생성, 추천 조합 : CUST_{userId}_{rand6:난수}
        // 임시 구현
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("email", email);
        response.put("customerUid", "CUST_" + Math.abs(email.hashCode()));  // PortOne 고객 UID
        response.put("name", email.split("@")[0]);  // 이메일에서 이름 추출
        response.put("phone", "010-0000-0000");  // Kg 이니시스 전화번호 필수
        response.put("pointBalance", 1000L);  // 포인트 잔액

        return ResponseEntity.ok(response);
    }

     */
}
