package com.magamsale.store.config;

import com.magamsale.store.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // ✅ REST API: 세션 사용 안함
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ✅ CSRF 비활성
                .csrf(csrf -> csrf.disable())

                // ✅ CORS 허용 (아래 corsConfigurationSource 빈 사용)
                .cors(Customizer.withDefaults())

                // ✅ 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // Preflight(OPTIONS) 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Swagger / docs 허용
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**"
                        ).permitAll()

                        // 로그인/회원가입 등 토큰 없는 API 허용
                        .requestMatchers(
                                "/api/seller/login",
                                "/api/seller/signup",
                                "/api/refresh",
                                "/api/seller/refresh",
                                "/api/seller/find-id",
                                "/api/seller/find-pw",
                                "/api/seller/reset-pw",
                                "/api/auth/**", // ⭐ 카카오 등 소셜 로그인 관련 경로 추가 권장
                                "/api/reservations/**", // 예약 API 허용
                                "/error"
                        ).permitAll()

                        //헬스 체크용 주소 : 무조건 허용
                        .requestMatchers("/api/health").permitAll()

                        // 제품/업로드 허용
                        .requestMatchers(
                                "/api/product/list",
                                "/api/product/**",
                                "/uploads/**"
                        ).permitAll()

                        // ✅ 나머지는 인증 필요
                        .anyRequest().authenticated()
                )

                // ✅ JWT 필터 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        http
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * ✅ CORS 설정 (핵심 수정)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 1. [수정됨] 허용할 오리진 (프론트엔드 주소)
        // 기존 setAllowedOrigins 대신 setAllowedOriginPatterns를 사용하여 모든 주소(*) 허용
        // 이유: AWS Fargate IP가 배포할 때마다 바뀌기 때문에 특정 IP만 적으면 에러가 납니다.
//      config.setAllowedOriginPatterns(List.of("*"));

        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "https://magamsale-jenuks.vercel.app"
        ));

        // 2. 허용할 HTTP 메서드 (PATCH 포함 필수)
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 3. [중요] 허용할 헤더: "*"로 설정하여 X-ACTOR-TYPE 등 모든 커스텀 헤더 허용!
        config.setAllowedHeaders(List.of("*"));

        // 4. 브라우저에 노출할 헤더
        config.setExposedHeaders(List.of("Authorization", "X-Actor-Uid"));

        // 5. 자격 증명(쿠키/헤더) 허용
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}