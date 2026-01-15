package com.magamsale.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    // ✅ SecurityConfig에 있던 녀석을 여기로 피신시켰습니다.
    // 이제 SellerService는 SecurityConfig가 아니라 이 파일을 의존하게 됩니다.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}