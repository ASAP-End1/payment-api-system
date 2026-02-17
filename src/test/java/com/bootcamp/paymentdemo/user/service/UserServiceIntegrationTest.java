package com.bootcamp.paymentdemo.user.service;

import com.bootcamp.paymentdemo.membership.entity.Membership;
import com.bootcamp.paymentdemo.membership.entity.MembershipGrade;
import com.bootcamp.paymentdemo.membership.repository.MembershipRepository;
import com.bootcamp.paymentdemo.security.entity.AccessTokenBlacklist;
import com.bootcamp.paymentdemo.security.entity.RefreshToken;
import com.bootcamp.paymentdemo.security.repository.AccessTokenBlacklistRepository;
import com.bootcamp.paymentdemo.security.repository.RefreshTokenRepository;
import com.bootcamp.paymentdemo.user.dto.SignupRequest;
import com.bootcamp.paymentdemo.user.dto.SignupResponse;
import com.bootcamp.paymentdemo.user.dto.UserSearchResponse;
import com.bootcamp.paymentdemo.user.entity.User;
import com.bootcamp.paymentdemo.user.entity.UserGradeHistory;
import com.bootcamp.paymentdemo.user.entity.UserPaidAmount;
import com.bootcamp.paymentdemo.user.entity.UserPointBalance;
import com.bootcamp.paymentdemo.user.exception.DuplicateEmailException;
import com.bootcamp.paymentdemo.user.exception.InvalidCredentialsException;
import com.bootcamp.paymentdemo.user.exception.UserNotFoundException;
import com.bootcamp.paymentdemo.user.repository.UserGradeHistoryRepository;
import com.bootcamp.paymentdemo.user.repository.UserPaidAmountRepository;
import com.bootcamp.paymentdemo.user.repository.UserPointBalanceRepository;
import com.bootcamp.paymentdemo.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPointBalanceRepository pointBalanceRepository;

    @Autowired
    private UserPaidAmountRepository paidAmountRepository;

    @Autowired
    private UserGradeHistoryRepository gradeHistoryRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private AccessTokenBlacklistRepository blacklistRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Membership normalGrade;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM access_token_blacklists");
        jdbcTemplate.execute("DELETE FROM refresh_tokens");
        jdbcTemplate.execute("DELETE FROM order_products");
        jdbcTemplate.execute("DELETE FROM payments");
        jdbcTemplate.execute("DELETE FROM refund_history");
        jdbcTemplate.execute("DELETE FROM orders");
        jdbcTemplate.execute("DELETE FROM point_transactions");
        jdbcTemplate.execute("DELETE FROM user_grade_histories");
        jdbcTemplate.execute("DELETE FROM user_point_balances");
        jdbcTemplate.execute("DELETE FROM user_paid_amounts");
        jdbcTemplate.execute("DELETE FROM users");

        entityManager.flush();
        entityManager.clear();

        normalGrade = membershipRepository.findByGradeName(MembershipGrade.NORMAL)
                .orElseThrow(() -> new IllegalStateException("NORMAL 등급이 없습니다"));
    }

    @Test
    @DisplayName("회원가입 성공 - 실제 DB 저장 확인")
    void signup_Success() {
        // given
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "name", "신규유저");
        ReflectionTestUtils.setField(request, "email", "newuser@test.com");
        ReflectionTestUtils.setField(request, "password", "password123");
        ReflectionTestUtils.setField(request, "phone", "010-9876-5432");

        // when
        SignupResponse response = userService.signup(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("newuser@test.com");
        assertThat(response.getUserId()).isNotNull();

        // DB 확인
        User savedUser = userRepository.findByEmail("newuser@test.com").orElseThrow();
        assertThat(savedUser.getEmail()).isEqualTo("newuser@test.com");
        assertThat(savedUser.getUsername()).isEqualTo("신규유저");
        assertThat(savedUser.getCurrentGrade().getGradeName()).isEqualTo(MembershipGrade.NORMAL);

        // 포인트 잔액 확인
        UserPointBalance pointBalance = pointBalanceRepository.findByUserId(savedUser.getUserId()).orElseThrow();
        assertThat(pointBalance.getCurrentPoints()).isEqualByComparingTo(BigDecimal.ZERO);

        // 누적 결제 금액 확인
        UserPaidAmount paidAmount = paidAmountRepository.findByUserId(savedUser.getUserId()).orElseThrow();
        assertThat(paidAmount.getTotalPaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        // 등급 이력 확인
        List<UserGradeHistory> histories = gradeHistoryRepository.findByUserUserIdOrderByUpdatedAtAsc(savedUser.getUserId());
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getReason()).isEqualTo("회원가입");
        assertThat(histories.get(0).getFromGrade()).isNull();
        assertThat(histories.get(0).getToGrade().getGradeName()).isEqualTo(MembershipGrade.NORMAL);
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복 (실제 DB 확인)")
    void signup_Fail_EmailDuplicated() {
        // given
        SignupRequest firstRequest = new SignupRequest();
        ReflectionTestUtils.setField(firstRequest, "name", "첫번째유저");
        ReflectionTestUtils.setField(firstRequest, "email", "duplicate@test.com");
        ReflectionTestUtils.setField(firstRequest, "password", "password123");
        ReflectionTestUtils.setField(firstRequest, "phone", "010-1111-1111");

        userService.signup(firstRequest);

        SignupRequest duplicateRequest = new SignupRequest();
        ReflectionTestUtils.setField(duplicateRequest, "name", "두번째유저");
        ReflectionTestUtils.setField(duplicateRequest, "email", "duplicate@test.com");
        ReflectionTestUtils.setField(duplicateRequest, "password", "password456");
        ReflectionTestUtils.setField(duplicateRequest, "phone", "010-2222-2222");

        // when & then
        assertThatThrownBy(() -> userService.signup(duplicateRequest))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("이미 사용 중인 이메일입니다.");

        // DB 확인 - 하나만 저장되어 있어야 함
        List<User> users = userRepository.findAll();
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getEmail()).isEqualTo("duplicate@test.com");
        assertThat(users.get(0).getUsername()).isEqualTo("첫번째유저");
    }

    @Test
    @DisplayName("로그인 성공")
    void login_Success_TokenSavedInDatabase() {
        // given - 회원가입
        SignupRequest signupRequest = new SignupRequest();
        ReflectionTestUtils.setField(signupRequest, "name", "로그인유저");
        ReflectionTestUtils.setField(signupRequest, "email", "loginuser@test.com");
        ReflectionTestUtils.setField(signupRequest, "password", "password123");
        ReflectionTestUtils.setField(signupRequest, "phone", "010-1111-1111");

        userService.signup(signupRequest);

        // when
        UserService.TokenPair tokenPair = userService.login("loginuser@test.com", "password123");

        // then
        assertThat(tokenPair).isNotNull();
        assertThat(tokenPair.accessToken).isNotNull();
        assertThat(tokenPair.refreshToken).isNotNull();
        assertThat(tokenPair.email).isEqualTo("loginuser@test.com");

        // DB 확인 - Refresh Token 저장되어 있어야 함
        List<RefreshToken> refreshTokens = refreshTokenRepository.findAll();
        assertThat(refreshTokens).hasSize(1);
        assertThat(refreshTokens.get(0).getToken()).isEqualTo(tokenPair.refreshToken);
        assertThat(refreshTokens.get(0).getRevoked()).isFalse();
        assertThat(refreshTokens.get(0).getUser().getEmail()).isEqualTo("loginuser@test.com");
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_Fail_PasswordMismatch() {
        // given - 회원가입
        SignupRequest signupRequest = new SignupRequest();
        ReflectionTestUtils.setField(signupRequest, "name", "불일치유저");
        ReflectionTestUtils.setField(signupRequest, "email", "bcryptuser@test.com");
        ReflectionTestUtils.setField(signupRequest, "password", "password123");
        ReflectionTestUtils.setField(signupRequest, "phone", "010-1111-1111");

        userService.signup(signupRequest);

        // when & then - 잘못된 비밀번호로 로그인 시도
        assertThatThrownBy(() -> userService.login("bcryptuser@test.com", "wrongpassword"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");

        // DB 확인 - Refresh Token이 저장되지 않아야 함
        List<RefreshToken> refreshTokens = refreshTokenRepository.findAll();
        assertThat(refreshTokens).isEmpty();
    }

    @Test
    @DisplayName("로그인 실패 - 사용자 없음")
    void login_Fail_UserNotFound() {
        // given

        // when & then
        assertThatThrownBy(() -> userService.login("notuser@test.com", "password123"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("다중 로그인 - 기존 Refresh Token 무효화 확인")
    void multipleLogin_RevokesPreviousRefreshTokens() {
        // given - 회원가입
        SignupRequest signupRequest = new SignupRequest();
        ReflectionTestUtils.setField(signupRequest, "name", "다중로그인유저");
        ReflectionTestUtils.setField(signupRequest, "email", "multilogin@test.com");
        ReflectionTestUtils.setField(signupRequest, "password", "password123");
        ReflectionTestUtils.setField(signupRequest, "phone", "010-1111-1111");

        userService.signup(signupRequest);

        // when - 첫 번째 로그인
        UserService.TokenPair firstLogin = userService.login("multilogin@test.com", "password123");

        entityManager.flush();   // 변경사항 DB 반영
        entityManager.clear();   // 영속성 컨텍스트 클리어

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // 두 번째 로그인
        UserService.TokenPair secondLogin = userService.login("multilogin@test.com", "password123");

        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(firstLogin.refreshToken).isNotEqualTo(secondLogin.refreshToken);

        // DB 확인 - 첫 번째 Refresh Token은 무효화되고, 두 번째만 유효해야 함
        List<RefreshToken> allTokens = refreshTokenRepository.findAll();
        assertThat(allTokens).hasSize(2);

        // 모든 토큰이 무효화되어야 함 (첫 번째 로그인 시 무효화 + 두 번째 로그인 시 첫 번째 무효화)
        long revokedCount = allTokens.stream().filter(RefreshToken::getRevoked).count();
        long activeCount = allTokens.stream().filter(token -> !token.getRevoked()).count();

        assertThat(revokedCount).isEqualTo(1);
        assertThat(activeCount).isEqualTo(1);

        // 두 번째 로그인 토큰이 유효해야 함
        RefreshToken activeToken = allTokens.stream()
                .filter(token -> !token.getRevoked())
                .findFirst()
                .orElseThrow();
        assertThat(activeToken.getToken()).isEqualTo(secondLogin.refreshToken);
    }

    @Test
    @DisplayName("로그아웃 성공 - Refresh Token 무효화 & Access Token 블랙리스트 추가 확인")
    void logout_Success_WithDatabaseUpdates() {
        // given
        SignupRequest signupRequest = new SignupRequest();
        ReflectionTestUtils.setField(signupRequest, "name", "로그아웃유저");
        ReflectionTestUtils.setField(signupRequest, "email", "logoutuser@test.com");
        ReflectionTestUtils.setField(signupRequest, "password", "password123");
        ReflectionTestUtils.setField(signupRequest, "phone", "010-1111-1111");

        userService.signup(signupRequest);
        UserService.TokenPair tokenPair = userService.login("logoutuser@test.com", "password123");

        // when
        userService.logout("logoutuser@test.com", tokenPair.accessToken);

        entityManager.flush();
        entityManager.clear();

        // then
        // Refresh Token 무효화 확인
        List<RefreshToken> refreshTokens = refreshTokenRepository.findAll();
        assertThat(refreshTokens).hasSize(1);
        assertThat(refreshTokens.get(0).getRevoked()).isTrue();

        // Access Token 블랙리스트 추가 확인
        List<AccessTokenBlacklist> blacklist = blacklistRepository.findAll();
        assertThat(blacklist).hasSize(1);
        assertThat(blacklist.get(0).getToken()).isEqualTo(tokenPair.accessToken);
        assertThat(blacklist.get(0).getEmail()).isEqualTo("logoutuser@test.com");
    }

    @Test
    @DisplayName("사용자 정보 조회 성공")
    void getCurrentUser_Success() {
        // given
        SignupRequest signupRequest = new SignupRequest();
        ReflectionTestUtils.setField(signupRequest, "name", "정보조회유저");
        ReflectionTestUtils.setField(signupRequest, "email", "getuser@test.com");
        ReflectionTestUtils.setField(signupRequest, "password", "password123");
        ReflectionTestUtils.setField(signupRequest, "phone", "010-1111-1111");
        userService.signup(signupRequest);

        userService.login("getuser@test.com", "password123");

        // when
        UserSearchResponse response = userService.getCurrentUser("getuser@test.com");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("getuser@test.com");
        assertThat(response.getName()).isEqualTo("정보조회유저");
        assertThat(response.getPhone()).isEqualTo("010-1111-1111");
        assertThat(response.getPointBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getTotalPaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getCurrentGrade()).isEqualTo("NORMAL");
    }

    @Test
    @DisplayName("사용자 정보 조회 실패 - 사용자 없음")
    void getCurrentUser_Fail_WhenUserNotFound_WithRealDatabase() {
        // given

        // when & then
        assertThatThrownBy(() -> userService.getCurrentUser("notexist@test.com"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("전체 시나리오 - 회원가입 → 로그인 → 정보조회 → 로그아웃")
    void fullScenario_SignupLoginGetUserLogout() {
        // given - 회원가입
        SignupRequest signupRequest = new SignupRequest();
        ReflectionTestUtils.setField(signupRequest, "name", "시나리오유저");
        ReflectionTestUtils.setField(signupRequest, "email", "total@test.com");
        ReflectionTestUtils.setField(signupRequest, "password", "password123");
        ReflectionTestUtils.setField(signupRequest, "phone", "010-1111-1111");

        SignupResponse signupResponse = userService.signup(signupRequest);
        assertThat(signupResponse).isNotNull();

        // when & then - 로그인
        UserService.TokenPair tokenPair = userService.login("total@test.com", "password123");
        assertThat(tokenPair.accessToken).isNotNull();
        assertThat(tokenPair.refreshToken).isNotNull();

        // when & then - 사용자 정보 조회
        UserSearchResponse userInfo = userService.getCurrentUser("total@test.com");
        assertThat(userInfo.getEmail()).isEqualTo("total@test.com");
        assertThat(userInfo.getName()).isEqualTo("시나리오유저");

        // when & then - 로그아웃
        userService.logout("total@test.com", tokenPair.accessToken);

        entityManager.flush();
        entityManager.clear();

        // 최종 DB 상태 확인
        List<RefreshToken> refreshTokens = refreshTokenRepository.findAll();
        assertThat(refreshTokens).hasSize(1);
        assertThat(refreshTokens.get(0).getRevoked()).isTrue();

        List<AccessTokenBlacklist> blacklist = blacklistRepository.findAll();
        assertThat(blacklist).hasSize(1);
        assertThat(blacklist.get(0).getToken()).isEqualTo(tokenPair.accessToken);
    }

}