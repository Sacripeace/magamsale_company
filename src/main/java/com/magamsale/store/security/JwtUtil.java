package com.magamsale.store.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {

    @Value("${jwt.signing.key}")
    private String signingKey;

    private final long ACCESS_TOKEN_EXPIRATION = 1000L * 60 * 15;            // 15분
    private final long REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 60 * 24 * 14; // 14일
    private final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS512;

    /**
     * ✅ SellerController가 호출하는 메서드 (없어서 컴파일 에러났던 부분)
     * - 내부적으로 기존 generateAccessToken / generateRefreshToken 을 그대로 사용
     */
    public String createAccessToken(Integer uid, String role) {
        // role이 "SELLER" 로 오면 토큰에는 "ROLE_SELLER"로 저장
        String r = (role == null) ? "ROLE_SELLER"
                : (role.startsWith("ROLE_") ? role : "ROLE_" + role);

        return generateAccessToken(uid, List.of(r));
    }

    public String createRefreshToken(Integer uid, String role) {
        // refresh token은 subject(uid)만 있어도 됨
        return generateRefreshToken(uid);
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(signingKey.getBytes(StandardCharsets.UTF_8));
    }

    private String normalizeToken(String tokenOrBearer) {
        if (tokenOrBearer == null) return null;
        if (tokenOrBearer.startsWith("Bearer ")) {
            return tokenOrBearer.substring("Bearer ".length()).trim();
        }
        return tokenOrBearer.trim();
    }

    private Claims extractClaims(String tokenOrBearer) {
        String token = normalizeToken(tokenOrBearer);
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Access Token 생성 (userUid + roles)
    public String generateAccessToken(int userUid, List<String> roles) {
        return Jwts.builder()
                .claim("userUid", userUid)
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
                .signWith(getSigningKey(), SIGNATURE_ALGORITHM)
                .compact();
    }

    // Refresh Token 생성 (subject = uid)
    public String generateRefreshToken(int userUid) {
        return Jwts.builder()
                .setSubject(String.valueOf(userUid))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(getSigningKey(), SIGNATURE_ALGORITHM)
                .compact();
    }

    public boolean isAccessTokenExpired(String tokenOrBearer) {
        try {
            Claims claims = extractClaims(tokenOrBearer);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public boolean isRefreshTokenExpired(String refreshTokenOrBearer) throws SignatureException {
        Claims claims = extractClaims(refreshTokenOrBearer);
        return claims.getExpiration().before(new Date());
    }

    public boolean isTokenValid(String tokenOrBearer) {
        try {
            extractClaims(tokenOrBearer);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int getUserUid(String tokenOrBearer) {
        return extractClaims(tokenOrBearer).get("userUid", Integer.class);
    }

    public List<String> getRoles(String tokenOrBearer) {
        Object raw = extractClaims(tokenOrBearer).get("roles");
        if (raw == null) return Collections.emptyList();

        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return Collections.emptyList();
    }

    public String getRole(String tokenOrBearer) {
        List<String> roles = getRoles(tokenOrBearer);
        if (roles.isEmpty()) return null;

        String r = roles.get(0);
        if (r == null) return null;
        if (r.startsWith("ROLE_")) return r.substring("ROLE_".length());
        return r;
    }
}