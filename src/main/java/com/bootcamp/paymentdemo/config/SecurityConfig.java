package com.bootcamp.paymentdemo.config;

import com.bootcamp.paymentdemo.common.dto.ApiResponse;
import com.bootcamp.paymentdemo.security.filter.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.boot.security.autoconfigure.web.servlet.PathRequest.toStaticResources;

/**
 * Spring Security 설정 - JWT 기반 인증 및 배포 환경 최적화
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, CorsConfigurationSource corsConfigurationSource, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.corsConfigurationSource = corsConfigurationSource;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 보안 기능 비활성화 및 설정
                .csrf(AbstractHttpConfigurer::disable) // JWT 사용으로 CSRF 비활성화
                .cors(cors -> cors.configurationSource(corsConfigurationSource)) // CORS 적용
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 세션 미사용
                )

                // 2. 예외 처리 (인증 실패 및 권한 부족)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            ApiResponse<Void> apiResponse = ApiResponse.error(HttpStatus.UNAUTHORIZED, "Authentication required");
                            response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            ApiResponse<Void> apiResponse = ApiResponse.error(HttpStatus.FORBIDDEN, "Access denied");
                            response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                        })
                )

                // 3. 경로별 권한 설정
                .authorizeHttpRequests(authorize -> authorize
                        // 1) 정적 리소스 및 기본 페이지 (배포 환경에서 누락되기 쉬운 경로들)
                        .requestMatchers(toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/", "/index.html", "/favicon.ico", "/css/**", "/js/**", "/assets/**", "/lib/**").permitAll()

                        // 2) 뷰 페이지 접근 허용
                        .requestMatchers(HttpMethod.GET, "/pages/**", "/login", "/signup").permitAll()

                        // 3) 공용 API 및 웹훅
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/portone-webhook").permitAll()

                        // 4) 인증 API (회원가입, 로그인) - 메서드 구분 없이 모두 허용으로 변경
                        .requestMatchers("/api/login", "/api/signup").permitAll()

                        // 5) 인증이 필요한 API
                        .requestMatchers("/api/logout", "/api/me").authenticated()
                        .requestMatchers("/api/**").authenticated()

                        // 6) 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // 4. JWT 필터 배치
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}