package com.bootcamp.paymentdemo.user.service;

import com.bootcamp.paymentdemo.membership.entity.Membership;
import com.bootcamp.paymentdemo.membership.entity.MembershipGrade;
import com.bootcamp.paymentdemo.membership.repository.MembershipRepository;
import com.bootcamp.paymentdemo.security.entity.AccessTokenBlacklist;
import com.bootcamp.paymentdemo.security.entity.RefreshToken;
import com.bootcamp.paymentdemo.security.provider.JwtTokenProvider;
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
import com.bootcamp.paymentdemo.user.exception.GradeNotFoundException;
import com.bootcamp.paymentdemo.user.exception.InvalidCredentialsException;
import com.bootcamp.paymentdemo.user.exception.UserNotFoundException;
import com.bootcamp.paymentdemo.user.repository.UserGradeHistoryRepository;
import com.bootcamp.paymentdemo.user.repository.UserPaidAmountRepository;
import com.bootcamp.paymentdemo.user.repository.UserPointBalanceRepository;
import com.bootcamp.paymentdemo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPointBalanceRepository userPointBalanceRepository;

    @Mock
    private UserPaidAmountRepository userPaidAmountRepository;

    @Mock
    private UserGradeHistoryRepository userGradeHistoryRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AccessTokenBlacklistRepository blacklistRepository;

    @Mock
    private UserSearchResponse userSearchResponse;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Membership normalGrade;

    @BeforeEach
    void setUp() {
        normalGrade = mock(Membership.class);
        ReflectionTestUtils.setField(normalGrade, "gradeName", MembershipGrade.NORMAL);
        ReflectionTestUtils.setField(normalGrade, "accRate", BigDecimal.valueOf(1));
        ReflectionTestUtils.setField(normalGrade, "minAmount", BigDecimal.ZERO);

        membershipRepository.save(normalGrade);

        testUser = User.register(
                "test@example.com",
                "encodedPassword",
                "테스트유저",
                "010-1234-5678",
                normalGrade
        );
    }

    @Test
    @DisplayName("회원가입 성공")
    void signup_Success(){
        // given
        SignupRequest request = new SignupRequest();

        given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(membershipRepository.findByGradeName(MembershipGrade.NORMAL)).willReturn(Optional.of(normalGrade));
        given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willReturn(testUser);
        given(userPointBalanceRepository.save(any(UserPointBalance.class))).willReturn(UserPointBalance.createDefault(testUser));
        given(userPaidAmountRepository.save(any(UserPaidAmount.class))).willReturn(UserPaidAmount.createDefault(testUser));
        given(userGradeHistoryRepository.save(any(UserGradeHistory.class))).willReturn(UserGradeHistory.createInitial(testUser, normalGrade));

        // when
        SignupResponse response = userService.signup(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@example.com");

        verify(userRepository).existsByEmail(request.getEmail());
        verify(passwordEncoder).encode(request.getPassword());
        verify(userRepository).save(any(User.class));
        verify(userPointBalanceRepository).save(any(UserPointBalance.class));
        verify(userPaidAmountRepository).save(any(UserPaidAmount.class));
        verify(userGradeHistoryRepository).save(any(UserGradeHistory.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signup_Fail_EmailDuplication(){
        // given
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "name", "중복유저");
        ReflectionTestUtils.setField(request, "email", "duplicate@test.com");
        ReflectionTestUtils.setField(request, "password", "password123");
        ReflectionTestUtils.setField(request, "phone", "010-1111-2222");

        given(userRepository.existsByEmail(request.getEmail())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.signup(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("이미 사용 중인 이메일입니다.");

        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 기본 등급 미존재")
    void signup_Fail_NoDefaultGrade(){
        // given
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "name", "신규유저");
        ReflectionTestUtils.setField(request, "email", "newuser@test.com");
        ReflectionTestUtils.setField(request, "password", "password123");
        ReflectionTestUtils.setField(request, "phone", "010-9876-5432");

        given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(membershipRepository.findByGradeName(MembershipGrade.NORMAL)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.signup(request))
                .isInstanceOf(GradeNotFoundException.class)
                .hasMessage("기본 등급을 찾을 수 없습니다.");

        verify(membershipRepository).findByGradeName(MembershipGrade.NORMAL);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("로그인 성공")
    void login_Success(){
        // given
        String email = "test@example.com";
        String password = "password123";

        given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches(password, testUser.getPassword())).willReturn(true);
        given(jwtTokenProvider.createAccessToken(email)).willReturn("access-token-12345");
        given(jwtTokenProvider.createRefreshToken(email)).willReturn("refresh-token-67890");
        given(jwtTokenProvider.getRefreshTokenExpiryDate()).willReturn(LocalDateTime.now().plusDays(7));
        given(refreshTokenRepository.save(any(RefreshToken.class))).willReturn(null);

        // when
        UserService.TokenPair result = userService.login(email, password);

        // then
        assertThat(result).isNotNull();
        assertThat(result.accessToken).isEqualTo("access-token-12345");
        assertThat(result.refreshToken).isEqualTo("refresh-token-67890");
        assertThat(result.email).isEqualTo(email);

        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches(password, testUser.getPassword());
        verify(refreshTokenRepository).revokeAllByUserId(testUser.getUserId());
        verify(jwtTokenProvider).createAccessToken(email);
        verify(jwtTokenProvider).createRefreshToken(email);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("로그인 실패 - 사용자 없음")
    void login_Fail_UserNotFound(){
        // given
        String email = "notuser@test.com";
        String password = "password123";

        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.login(email, password))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");

        verify(userRepository).findByEmail(email);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtTokenProvider, never()).createAccessToken(anyString());
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_Fail_WrongPassword(){
        // given
        String email = "test@test.com";
        String wrongPassword = "wrongpassword";

        given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches(wrongPassword, testUser.getPassword())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> userService.login(email, wrongPassword))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");

        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches(wrongPassword, testUser.getPassword());
        verify(jwtTokenProvider, never()).createAccessToken(anyString());
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_Success(){
        // given
        String email = "test@example.com";
        String accessToken = "access-token-12345";
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
        given(jwtTokenProvider.getExpirationDate(accessToken)).willReturn(expiresAt);
        given(blacklistRepository.save(any(AccessTokenBlacklist.class))).willReturn(null);

        // when
        userService.logout(email, accessToken);

        // then
        verify(userRepository).findByEmail(email);
        verify(refreshTokenRepository).revokeAllByUserId(testUser.getUserId());
        verify(jwtTokenProvider).getExpirationDate(accessToken);
        verify(blacklistRepository).save(any(AccessTokenBlacklist.class));
    }

    @Test
    @DisplayName("로그아웃 실패 - 사용자 없음")
    void logout_Fail_UserNotFound(){
        // given
        String email = "notexist@test.com";
        String accessToken = "access-token-12345";

        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.logout(email, accessToken))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");

        verify(userRepository).findByEmail(email);
        verify(refreshTokenRepository, never()).revokeAllByUserId(anyLong());
        verify(blacklistRepository, never()).save(any(AccessTokenBlacklist.class));
    }

    @Test
    @DisplayName("사용자 정보 조회 성공")
    void getCurrentUser_Success(){
        // given
        String email = "test@example.com";
        UserPointBalance pointBalance = UserPointBalance.createDefault(testUser);
        UserPaidAmount paidAmount = UserPaidAmount.createDefault(testUser);

        given(userRepository.findByEmailWithGrade(email)).willReturn(Optional.of(testUser));
        given(userPointBalanceRepository.findByUserId(testUser.getUserId())).willReturn(Optional.of(pointBalance));
        given(userPaidAmountRepository.findByUserId(testUser.getUserId())).willReturn(Optional.of(paidAmount));

        // when
        when(normalGrade.getGradeName()).thenReturn(MembershipGrade.NORMAL);
        UserSearchResponse response = userService.getCurrentUser(email);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo(email);

        verify(userRepository).findByEmailWithGrade(email);
        verify(userPointBalanceRepository).findByUserId(testUser.getUserId());
        verify(userPaidAmountRepository).findByUserId(testUser.getUserId());
    }

    @Test
    @DisplayName("사용자 정보 조회 실패 - 사용자 없음")
    void getCurrentUser_Fail_UserNotFound(){
        // given
        String email = "notuser@test.com";

        given(userRepository.findByEmailWithGrade(email)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getCurrentUser(email))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");

        verify(userRepository).findByEmailWithGrade(email);
        verify(userPointBalanceRepository, never()).findByUserId(anyLong());
    }


}