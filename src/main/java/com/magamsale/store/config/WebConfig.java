package com.magamsale.store.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // ❌ [삭제] addCorsMappings 제거 (SecurityConfig와 충돌 방지)

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 업로드 파일 경로 매핑
        String uploadPath = System.getProperty("user.dir") + "/uploads/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);
    }
}