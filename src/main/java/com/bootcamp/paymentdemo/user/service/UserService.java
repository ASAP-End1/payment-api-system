package com.bootcamp.paymentdemo.user.service;

import com.bootcamp.paymentdemo.membership.entity.Membership;
import com.bootcamp.paymentdemo.membership.entity.MembershipGrade;
import com.bootcamp.paymentdemo.membership.repository.MembershipRepository;
import com.bootcamp.paymentdemo.security.entity.AccessTokenBlacklist;
import com.bootcamp.paymentdemo.security.entity.RefreshToken;
import com.bootcamp.paymentdemo.security.provider.JwtTokenProvider;
import com.bootcamp.paymentdemo.security.repository.AccessTokenBlacklistRepository;
import com.bootcamp.paymentdemo.security.repository.RefreshTokenRepository;
import com.bootcamp.paymentdemo.user.dto.*;
import com.bootcamp.paymentdemo.user.entity.*;
import com.bootcamp.paymentdemo.user.exception.*;
import com.bootcamp.paymentdemo.user.repository.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final AccessTokenBlacklistRepository blacklistRepository;

    private static final MembershipGrade DEFAULT_GRADE = MembershipGrade.NORMAL;

    // 회원가입
    @Transactional
    public SignupResponse signup(@Valid SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
        }

        Membership defaultGrade = gradeRepository.findByGradeName(DEFAULT_GRADE).orElseThrow(
                () -> new GradeNotFoundException("기본 등급을 찾을 수 없습니다.")
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

        return new SignupResponse(saveUser.getUserId(), saveUser.getEmail());
    }

    // 로그인
    @Transactional
    public TokenPair login(String email, String password) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new UserNotFoundException("사용자를 찾을 수 없습니다.")
        );

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

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


    // 로그아웃
    @Transactional
    public void logout(String email, String accessToken) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new UserNotFoundException("사용자를 찾을 수 없습니다.")
        );

        // 해당 사용자의 모든 Refresh Token 무효화
        refreshTokenRepository.revokeAllByUserId(user.getUserId());

        // Access Token 블랙리스트에 추가
        LocalDateTime expiresAt = jwtTokenProvider.getExpirationDate(accessToken);
        AccessTokenBlacklist blacklist = AccessTokenBlacklist.create(accessToken, email, expiresAt);
        blacklistRepository.save(blacklist);

        log.info("로그아웃 완료: email={}, userId={}", user.getEmail(), user.getUserId());
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
