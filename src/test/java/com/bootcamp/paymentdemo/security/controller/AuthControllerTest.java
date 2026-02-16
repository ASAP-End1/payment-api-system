package com.bootcamp.paymentdemo.security.controller;

import com.bootcamp.paymentdemo.security.provider.JwtTokenProvider;
import com.bootcamp.paymentdemo.security.repository.AccessTokenBlacklistRepository;
import com.bootcamp.paymentdemo.security.repository.RefreshTokenRepository;
import com.bootcamp.paymentdemo.user.dto.LoginRequest;
import com.bootcamp.paymentdemo.user.dto.SignupRequest;
import com.bootcamp.paymentdemo.user.dto.SignupResponse;
import com.bootcamp.paymentdemo.user.dto.UserSearchResponse;
import com.bootcamp.paymentdemo.user.exception.DuplicateEmailException;
import com.bootcamp.paymentdemo.user.exception.InvalidCredentialsException;
import com.bootcamp.paymentdemo.user.exception.UserNotFoundException;
import com.bootcamp.paymentdemo.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    private AccessTokenBlacklistRepository blacklistRepository;

    private SignupResponse signupResponse;
    private UserService.TokenPair tokenPair;
    private UserSearchResponse userSearchResponse;

    // Principal 직접 주입
    private RequestPostProcessor principal(String username) {
        return request -> {
            request.setUserPrincipal(() -> username);
            return request;
        };
    }

    @BeforeEach
    void setUp() {
        // 테스트용 회원가입 응답
        signupResponse = new SignupResponse(1L, "test@test.com");

        // 테스트용 토큰
        tokenPair = new UserService.TokenPair(
                "access-token-12345",
                "refresh-token-67890",
                "test@test.com"
        );

        // 테스트용 사용자 정보
        userSearchResponse = new UserSearchResponse(
                1L,
                "test@test.com",
                "홍길동",
                "010-1234-5678",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "NORMAL"
        );

    }

    @Test
    @DisplayName("회원가입 성공 - 201 Created")
    void signup_Success() throws Exception {
        // given
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "name", "홍길동");
        ReflectionTestUtils.setField(request, "email", "test@test.com");
        ReflectionTestUtils.setField(request, "password", "password123");
        ReflectionTestUtils.setField(request, "phone", "010-1234-5678");

        given(userService.signup(any(SignupRequest.class))).willReturn(signupResponse);

        // when & then
        mockMvc.perform(post("/api/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("201 CREATED"))
                .andExpect(jsonPath("$.message").value("회원가입 성공"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.email").value("test@test.com"));

        verify(userService).signup(any(SignupRequest.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signup_Fail_EmailDuplication() throws Exception {
        // given
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "name", "홍길동");
        ReflectionTestUtils.setField(request, "email", "duplicate@test.com");
        ReflectionTestUtils.setField(request, "password", "password123");
        ReflectionTestUtils.setField(request, "phone", "010-1234-5678");

        given(userService.signup(any(SignupRequest.class)))
                .willThrow(new DuplicateEmailException("이미 사용 중인 이메일입니다"));

        // when & then
        mockMvc.perform(post("/api/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다"));

        verify(userService).signup(any(SignupRequest.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 입력값 유효성 검증" )
    void signup_Fail_Validation() throws Exception {
        // given
        SignupRequest invalidRequest = new SignupRequest();
        ReflectionTestUtils.setField(invalidRequest, "name", "");
        ReflectionTestUtils.setField(invalidRequest, "email", "testemail");
        ReflectionTestUtils.setField(invalidRequest, "password", "");
        ReflectionTestUtils.setField(invalidRequest, "phone", "010");

        // when & then
        mockMvc.perform(post("/api/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(userService, never()).signup(any(SignupRequest.class));
    }

    @Test
    @DisplayName("로그인 성공")
    void login_Sccess() throws Exception {
        // given
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "email", "test@test.com");
        ReflectionTestUtils.setField(request, "password", "password123");

        given(userService.login(anyString(), anyString())).willReturn(tokenPair);

        // when & then
        mockMvc.perform(post("/api/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.AUTHORIZATION, "Bearer access-token-12345"))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=refresh-token-67890")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=604800")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200 OK"))
                .andExpect(jsonPath("$.message").value("로그인 성공"))
                .andExpect(jsonPath("$.data.email").value("test@test.com"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token-12345"));

        verify(userService).login("test@test.com", "password123");
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_Fail_PasswordMismatch() throws Exception {
        // given
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "email", "test@test.com");
        ReflectionTestUtils.setField(request, "password", "wrongpassword");

        given(userService.login(anyString(), anyString()))
                .willThrow(new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다"));

        // when & then
        mockMvc.perform(post("/api/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다"));

        verify(userService).login("test@test.com", "wrongpassword");
    }

    @Test
    @DisplayName("로그인 실패 - 사용자 없음")
    void login_Fail_WhenUserNotFound_Returns404NotFound() throws Exception {
        // given
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "email", "notuser@test.com");
        ReflectionTestUtils.setField(request, "password", "password123");

        given(userService.login(anyString(), anyString()))
                .willThrow(new UserNotFoundException("사용자가 존재하지 않습니다"));

        // when & then
        mockMvc.perform(post("/api/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("사용자가 존재하지 않습니다"));

        verify(userService).login("notuser@test.com", "password123");
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_Success() throws Exception {
        // given
        willDoNothing().given(userService).logout(anyString(), anyString());

        // when & then
        mockMvc.perform(post("/api/logout")
                        .with(csrf())
                        .with(principal("test@test.com"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token-12345"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200 OK"))
                .andExpect(jsonPath("$.message").value("로그아웃되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(userService).logout(eq("test@test.com"), eq("access-token-12345"));
    }

    @Test
    @DisplayName("사용자 정보 조회 성공")
    void getCurrentUser_Success() throws Exception {
        // given
        given(userService.getCurrentUser(anyString())).willReturn(userSearchResponse);

        // when & then
        mockMvc.perform(get("/api/me")
                        .with(csrf())
                        .with(principal("test@test.com"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token-12345"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200 OK"))
                .andExpect(jsonPath("$.message").value("사용자 조회 성공"))
                .andExpect(jsonPath("$.data.email").value("test@test.com"))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.phone").value("010-1234-5678"))
                .andExpect(jsonPath("$.data.pointBalance").value(0))
                .andExpect(jsonPath("$.data.totalPaidAmount").value(0))
                .andExpect(jsonPath("$.data.currentGrade").value("NORMAL"));

        verify(userService).getCurrentUser("test@test.com");
    }

    @Test
    @DisplayName("사용자 정보 조회 실패 - 사용자 없음")
    void getCurrentUser_Fail_UserNotFound() throws Exception {
        // given
        given(userService.getCurrentUser(anyString()))
                .willThrow(new UserNotFoundException("사용자를 찾을 수 없습니다"));

        // when & then
        mockMvc.perform(get("/api/me")
                        .with(csrf())
                        .with(principal("notexist@test.com")))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다"));

        verify(userService).getCurrentUser("notexist@test.com");
    }


}