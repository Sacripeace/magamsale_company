package com.magamsale.store.controller;

import com.magamsale.store.entity.Seller;
import com.magamsale.store.security.JwtUtil;
import com.magamsale.store.service.SellerService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Access Token 재발급을 처리하는 컨트롤러
 * - /api/auth/refresh
 * - 쿠키에 담긴 refreshToken을 검증한 뒤
 *   새로운 AccessToken + RefreshToken을 발급
 */
@RestController
@RequestMapping("/api/auth")
public class RefreshTokenController {

    private final JwtUtil jwtUtil;
    private final SellerService sellerService;

    public RefreshTokenController(JwtUtil jwtUtil, SellerService sellerService) {
        this.jwtUtil = jwtUtil;
        this.sellerService = sellerService;
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        // 1) 쿠키에 리프레시 토큰이 없는 경우
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("RefreshToken이 존재하지 않습니다.");
        }

        // 2) 리프레시 토큰 만료/유효성 확인
        try {
            if (jwtUtil.isRefreshTokenExpired(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("RefreshToken이 만료되었습니다.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("유효하지 않은 RefreshToken 입니다.");
        }

        // 3) 헤더에서 기존 AccessToken 가져오기
        String accessToken = request.getHeader("Authorization");

        if (accessToken == null || !accessToken.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("AccessToken이 없습니다.");
        }

        // 4) AccessToken에서 UID 추출
        int sellerUid = jwtUtil.getUserUid(accessToken);

        // 5) DB에서 사장님 조회
        Seller seller = sellerService.getSellerByUid(sellerUid);

        if (seller == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("존재하지 않는 사용자입니다.");
        }

        // 6) DB에 저장된 리프레시 토큰과 비교
        if (seller.getRefreshToken() == null ||
                !refreshToken.equals(seller.getRefreshToken())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("RefreshToken이 일치하지 않습니다.");
        }

        // 7) 역할 세팅 (없으면 기본값)
        if (seller.getRoles() == null || seller.getRoles().isEmpty()) {
            seller.setRoles(java.util.List.of("ROLE_SELLER"));
        }

        // 8) 새 AccessToken 발급
        String newAccessToken = jwtUtil.generateAccessToken(
                sellerUid,
                seller.getRoles()
        );

        // 9) 새 RefreshToken 발급
        String newRefreshToken = jwtUtil.generateRefreshToken(sellerUid);

        // 10) DB에 새 RefreshToken 저장
        sellerService.updateRefreshToken(sellerUid, newRefreshToken);

        // 11) 쿠키에 새 RefreshToken 저장
        Cookie cookie = new Cookie("refreshToken", newRefreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // 배포 시 true 권장 (HTTPS)
        cookie.setPath("/");
        cookie.setMaxAge(14 * 24 * 60 * 60);
        response.addCookie(cookie);

        // 12) 새 AccessToken을 헤더에 담아서 내려줌
        response.addHeader("Authorization", "Bearer " + newAccessToken);

        return ResponseEntity.ok("토큰 재발급 완료");
    }
}