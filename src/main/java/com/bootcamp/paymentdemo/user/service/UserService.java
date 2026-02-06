package com.bootcamp.paymentdemo.user.service;

import com.bootcamp.paymentdemo.membership.entity.Membership;
import com.bootcamp.paymentdemo.membership.entity.MembershipGrade;
import com.bootcamp.paymentdemo.membership.repository.MembershipRepository;
import com.bootcamp.paymentdemo.security.JwtTokenProvider;
import com.bootcamp.paymentdemo.user.dto.SignupRequest;
import com.bootcamp.paymentdemo.user.dto.SignupResponse;
import com.bootcamp.paymentdemo.user.entity.User;
import com.bootcamp.paymentdemo.user.entity.UserPointBalance;
import com.bootcamp.paymentdemo.user.repository.RefreshTokenRepository;
import com.bootcamp.paymentdemo.user.repository.UserPointBalanceRepository;
import com.bootcamp.paymentdemo.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserPointBalanceRepository pointBalanceRepository;
    private final MembershipRepository gradeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    private static final String DEFAULT_GRADE = "NORMAL";

    // 회원가입
    @Transactional
    public SignupResponse signup(@Valid SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다");
        }

        Membership defaultGrade = gradeRepository.findByGradeName(DEFAULT_GRADE).orElseThrow(
                () -> new IllegalArgumentException("기본 등급을 찾을 수 없습니다")
        );

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.register(request.getEmail(), encodedPassword, request.getName(), request.getPhone(), defaultGrade);
        User saveUser = userRepository.save(user);

        // 포인트 잔액 초기화
        UserPointBalance pointBalance = UserPointBalance.createDefault(saveUser);
        pointBalanceRepository.save(pointBalance);

        log.info("회원가입 완료: email={}, userId={}", saveUser.getEmail(), saveUser.getUserId());

        return SignupResponse.success();
    }
}
