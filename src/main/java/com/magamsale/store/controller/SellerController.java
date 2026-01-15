package com.magamsale.store.controller;

import com.magamsale.store.dto.*;
import com.magamsale.store.entity.Seller;
import com.magamsale.store.exception.BadRequestException;
import com.magamsale.store.security.JwtUtil;
import com.magamsale.store.service.SellerService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;
    private final JwtUtil jwtUtil;

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SellerSignupDTO dto) {
        sellerService.createSeller(dto);
        return ResponseEntity.ok("사장님 회원등록에 성공하셨습니다.");
    }

    /**
     * ✅ 로그인
     * - 프론트가 기대하는 JSON 키(sellerId/storeName/accessToken)를 반드시 내려준다.
     * - refreshToken은 HttpOnly 쿠키로 내려준다.
     * - (선택) Authorization 헤더에도 accessToken을 내려준다. (프론트가 헤더에서 읽어도 됨)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody SellerLoginDTO dto, HttpServletResponse response) {

        // 1) 로그인 검증
        Seller seller = sellerService.login(dto.getUsername(), dto.getPassword());

        // 2) 토큰 발급
        String accessToken = jwtUtil.createAccessToken(seller.getUid(), "SELLER");
        String refreshToken = jwtUtil.createRefreshToken(seller.getUid(), "SELLER");

        // 3) refreshToken DB 저장
        sellerService.updateRefreshToken(seller.getUid(), refreshToken);

        // 4) refreshToken 쿠키
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false)          // 운영 HTTPS면 true 권장
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(14))
                .build();

        // 5) 프론트 규격 응답
        Map<String, Object> body = new HashMap<>();
        body.put("sellerId", seller.getUid());
        body.put("storeName", seller.getStoreName());
        body.put("accessToken", accessToken);
        body.put("role", "company");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                // ✅ 헤더로도 내려주면 프론트에서 대안으로 읽을 수 있음
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(body);
    }

    @GetMapping("/{uid:\\d+}")
    public SellerInfoResponse getSeller(@PathVariable int uid) {
        return sellerService.getSellerInfoByUid(uid);
    }

    @GetMapping("/me")
    public ResponseEntity<SellerInfoResponse> getMyStore(Authentication authentication) {
        // ⚠️ 여기 principal 캐스팅이 프로젝트에 따라 다를 수 있음.
        // 일단 403 문제부터 해결(토큰)되면, 여기서 500/캐스팅 문제가 있는지 확인.
        Seller loginSeller = (Seller) authentication.getPrincipal();
        return ResponseEntity.ok(sellerService.getMyStoreInfo(loginSeller.getUid()));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMyStore(@RequestBody SellerUpdateRequest req,
                                           Authentication authentication) {
        Seller loginSeller = (Seller) authentication.getPrincipal();
        sellerService.updateMyStoreInfo(loginSeller.getUid(), req);
        return ResponseEntity.ok("상점 정보 수정 완료");
    }

    @DeleteMapping("/{uid:\\d+}")
    public ResponseEntity<?> deleteSeller(@PathVariable int uid, Authentication auth) {
        Seller loginSeller = (Seller) auth.getPrincipal();

        // getUid() 타입이 int/Integer에 따라 비교 방식이 달라질 수 있어 안전하게 처리
        if (loginSeller.getUid() != uid) {
            return ResponseEntity.status(403).body("본인 계정만 탈퇴할 수 있습니다.");
        }
        sellerService.deleteSeller(uid);
        return ResponseEntity.ok("회원 탈퇴 완료");
    }

    @PostMapping("/find-id")
    public ResponseEntity<Map<String, String>> findId(@RequestBody Map<String, String> body) {
        String businessUid = body.getOrDefault("businessUid", body.getOrDefault("businessNumber", ""));
        String phoneNumber = body.getOrDefault("phoneNumber", "");
        String password = body.getOrDefault("password", "");

        String username = sellerService.findId(businessUid, phoneNumber, password);
        return ResponseEntity.ok(Map.of("username", username));
    }

    /**
     * ✅ [추가] 비밀번호 재설정 전 본인 확인
     * - 정보가 일치하면 200 OK
     * - 일치하지 않으면 400 Bad Request
     */
    @PostMapping("/find-pw")
    public ResponseEntity<?> findPw(@RequestBody FindPwRequest req) {
        try {
            sellerService.verifySellerForPwReset(req);
            return ResponseEntity.ok(Map.of("message", "사용자 확인 성공"));
        } catch (BadRequestException e) {
            // "일치하는 판매자가 없습니다" 등의 에러를 500이 아닌 400으로 내려줌
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    /**
     * ✅ [추가] 비밀번호 재설정 실행
     */
    @PostMapping("/reset-pw")
    public ResponseEntity<?> resetPw(@RequestBody ResetPwRequest req) {
        try {
            sellerService.resetPassword(req);
            return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
        } catch (BadRequestException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }


}