package com.magamsale.store.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * seller_tb와 매핑되는 사장님 엔티티
 * + JWT/스프링 시큐리티에서 쓸 권한(roles), refreshToken 필드 추가
 */
@Getter
@Setter
@Data
public class Seller {

    // DB 기본 컬럼들
    private Integer uid;
    private String id;
    private String pw;
    private String phoneNumber;
    private String storeName;
    private String businessUid;
    private String address;
    private String createdAt;

    private String refreshToken;
    private List<String> roles;

    private Double storeLat;   // 가게 위도
    private Double storeLng;   // 가게 경도

    /**
     * Spring Security가 이해할 수 있는 형태(GrantedAuthority)로 변환
     * roles가 비어 있으면 기본값으로 "ROLE_SELLER" 부여
     */
    public List<GrantedAuthority> getAuthorities() {
        List<String> roleList = this.roles;

        if (roleList == null || roleList.isEmpty()) {
            roleList = Collections.singletonList("ROLE_SELLER");
            this.roles = roleList;
        }

        return roleList.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}