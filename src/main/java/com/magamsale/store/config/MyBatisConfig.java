package com.magamsale.store.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "com.magamsale.store.repository")
public class MyBatisConfig {

    @Bean
    public org.apache.ibatis.session.Configuration mybatisConfiguration() {
        org.apache.ibatis.session.Configuration config =
                new org.apache.ibatis.session.Configuration();

//        이게 핵심!! 마이바티스에서는
        config.setMapUnderscoreToCamelCase(true);
        return config;
    }
}