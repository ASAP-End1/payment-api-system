package com.bootcamp.paymentdemo.config;

import com.bootcamp.paymentdemo.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.springframework.boot.security.autoconfigure.web.servlet.PathRequest.toStaticResources;

/**
 * Spring Security 설정 - JWT 기반 인증
 *
 * TODO: 개선 사항
 * - CORS 설정 추가
 * - 역할 기반 접근 제어 (ROLE_ADMIN, ROLE_USER)
 * - API 엔드포인트별 세밀한 권한 설정
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (JWT 사용 시 불필요)
            .csrf(AbstractHttpConfigurer::disable)

            // CORS 적용
            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            // Session 사용 안 함 (Stateless)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

                // 인증/인가 예외처리
                .exceptionHandling(exceptions -> exceptions
                        // 인증 실패 시 (401)
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write(
                                    "{\"code\":\"UNAUTHORIZED\"," +
                                            "\"message\":\"Authentication required\"," +
                                            "\"status\":401," +
                                            "\"timestamp\":" + System.currentTimeMillis() + "}"
                            );
                        })
                        // 권한 없음 시 (403)
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write(
                                    "{\"code\":\"FORBIDDEN\"," +
                                            "\"message\":\"Access denied\"," +
                                            "\"status\":403," +
                                            "\"timestamp\":" + System.currentTimeMillis() + "}"
                            );
                        })
                )

            // 요청 권한 설정
            .authorizeHttpRequests(authorize -> authorize
                     // 1) 정적 리소스
                    .requestMatchers(toStaticResources().atCommonLocations()).permitAll()

                    // 2) 템플릿 페이지 렌더링
                    .requestMatchers(HttpMethod.GET, "/").permitAll()
                    .requestMatchers(HttpMethod.GET, "/pages/**").permitAll()

                    // 3) 공개 API
                    .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()

                    // 4) 인증 API (회원가입, 로그인)
                    .requestMatchers(HttpMethod.POST, "/api/login", "/api/signup").permitAll()

                    // 5) 그 외 API는 인증 필요
                    .requestMatchers(HttpMethod.POST, "/api/logout").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/me").authenticated()
                    .requestMatchers("/api/**").authenticated()

                    // 6) 나머지 전부 인증 필요
                    .anyRequest().authenticated()
            )

            // JWT 필터 추가
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * PasswordEncoder Bean
     */
    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Admin 계정 (InMemory - 데모용)
     */
    /*
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails admin = User.builder()
            .username("admin@test.com")
            .password(passwordEncoder.encode("admin"))
            .roles("USER", "ADMIN")
            .build();

        return new InMemoryUserDetailsManager(admin);
    }
     */

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }
}
