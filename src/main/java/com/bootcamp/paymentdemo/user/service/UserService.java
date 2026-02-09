package com.bootcamp.paymentdemo.user.service;

import com.bootcamp.paymentdemo.membership.entity.Membership;
import com.bootcamp.paymentdemo.membership.entity.MembershipGrade;
import com.bootcamp.paymentdemo.membership.repository.MembershipRepository;
import com.bootcamp.paymentdemo.security.JwtTokenProvider;
import com.bootcamp.paymentdemo.user.dto.*;
import com.bootcamp.paymentdemo.user.entity.*;
import com.bootcamp.paymentdemo.user.exception.*;
import com.bootcamp.paymentdemo.user.repository.*;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserPointBalanceRepository pointBalanceRepository;
    private final MembershipRepository gradeRepository;
    private final UserPaidAmountRepository userPaidAmountRepository;
    private final UserGradeHistoryRepository userGradeHistoryRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    private static final MembershipGrade DEFAULT_GRADE = MembershipGrade.NORMAL;

    // 회원가입
    @Transactional
    public SignupResponse signup(@Valid SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다");
        }

        Membership defaultGrade = gradeRepository.findByGradeName(DEFAULT_GRADE).orElseThrow(
                () -> new GradeNotFoundException("기본 등급을 찾을 수 없습니다")
        );

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.register(request.getEmail(), encodedPassword, request.getName(), request.getPhone(), defaultGrade);
        User saveUser = userRepository.save(user);

        // 포인트 잔액 초기화
        UserPointBalance pointBalance = UserPointBalance.createDefault(saveUser);
        pointBalanceRepository.save(pointBalance);

        // 총 결제 금액 초기화
        UserPaidAmount paidAmount = UserPaidAmount.createDefault(saveUser);
        userPaidAmountRepository.save(paidAmount);

        // 등급 변경 이력 초기 생성
        UserGradeHistory history = UserGradeHistory.createInitial(saveUser, defaultGrade);
        userGradeHistoryRepository.save(history);

        log.info("회원가입 완료: email={}, userId={}", saveUser.getEmail(), saveUser.getUserId());

        return SignupResponse.success();
    }


    // 로그인 - 인증은 이미 AuthenticationManager가 수행
    @Transactional
    public TokenPair login(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new UserNotFoundException("사용자가 존재하지 않습니다")
        );

        // 기존 Refresh Token 모두 무효화
        refreshTokenRepository.revokeAllByUserId(user.getUserId());

        // Access Token & Refresh Token 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

        // Refresh Token DB 저장
        RefreshToken refreshTokenEntity = RefreshToken.createToken(
                user,
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpiryDate()
        );
        refreshTokenRepository.save(refreshTokenEntity);

        log.info("로그인 성공: email={}, userId={}", user.getEmail(), user.getUserId());

        return new TokenPair(accessToken, refreshToken, user.getEmail());
    }

    // LoginResponse 생성 헬퍼 메서드
    public LoginResponse createLoginResponse(String email) {

        return LoginResponse.success(email);
    }

    // 로그아웃 - Refresh Token 무효화
    @Transactional
    public LogoutResponse logout(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new UserNotFoundException("사용자가 존재하지 않습니다")
        );

        // 해당 사용자의 모든 Refresh Token 무효화
        refreshTokenRepository.revokeAllByUserId(user.getUserId());

        log.info("로그아웃 완료: email={}, userId={}", user.getEmail(), user.getUserId());

        return LogoutResponse.success();
    }

    // 내 정보 조회
    @Transactional(readOnly = true)
    public UserSearchResponse getCurrentUser(String email) {
        User user = userRepository.findByEmailWithGrade(email).orElseThrow(
                () -> new UserNotFoundException("사용자를 찾을 수 없습니다")
        );

        UserPointBalance pointBalance = pointBalanceRepository.findByUserId(user.getUserId())
                .orElseGet(() -> {
                    // 포인트 잔액이 없는 경우 기본값 생성
                    UserPointBalance newBalance = UserPointBalance.createDefault(user);
                    return pointBalanceRepository.save(newBalance);
                });

        UserPaidAmount paidAmount = userPaidAmountRepository.findByUserId(user.getUserId())
                .orElseGet(() -> {
                    UserPaidAmount newPaidAmount = UserPaidAmount.createDefault(user);
                    return userPaidAmountRepository.save(newPaidAmount);
                });

        return UserSearchResponse.from(user, pointBalance, paidAmount);
    }



    // Token Pair
    public static class TokenPair {
        public final String accessToken;
        public final String refreshToken;
        public final String email;

        public TokenPair(String accessToken, String refreshToken, String email) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.email = email;
        }
    }
}
