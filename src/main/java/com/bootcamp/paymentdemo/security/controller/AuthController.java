package com.bootcamp.paymentdemo.security.controller;

import com.bootcamp.paymentdemo.common.dto.ApiResponse;
import com.bootcamp.paymentdemo.user.dto.*;
import com.bootcamp.paymentdemo.user.exception.InvalidCredentialsException;
import com.bootcamp.paymentdemo.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
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


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;


    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        log.info("회원가입 요청: email={}", request.getEmail());
        SignupResponse response = userService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "회원가입 성공", response));
    }




    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("로그인 요청: email={}", request.getEmail());

        UserService.TokenPair tokenPair = userService.login(request.getEmail(), request.getPassword());

        LoginResponse response = new LoginResponse(tokenPair.email, tokenPair.accessToken);


        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPair.accessToken);


        ResponseCookie refreshCookie = ResponseCookie
                .from("refreshToken", tokenPair.refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Lax")
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .headers(headers)
                .body(ApiResponse.success(HttpStatus.OK, "로그인 성공", response));

    }


    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(Principal principal, HttpServletRequest request) {
        log.info("로그아웃 요청: email={}", principal.getName());

        String accessToken = extractAccessToken(request);

        userService.logout(principal.getName(), accessToken);


        ResponseCookie deleteCookie = ResponseCookie
                .from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(ApiResponse.success(HttpStatus.OK, "로그아웃되었습니다.", null));
    }


    private String extractAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }



    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserSearchResponse>> getCurrentUser(Principal principal) {
        log.info("현재 사용자 조회: email={}", principal.getName());

        UserSearchResponse response = userService.getCurrentUser(principal.getName());

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK, "사용자 조회 성공", response));
    }
}
