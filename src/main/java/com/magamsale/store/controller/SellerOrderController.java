package com.magamsale.store.controller;

import com.magamsale.store.dto.ReservationResponse;
import com.magamsale.store.repository.ReservationRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/seller/orders")
@RequiredArgsConstructor
public class SellerOrderController {

    private final ReservationRepository reservationRepository;

    @GetMapping("/latest")
    public ResponseEntity<?> getLatestOrder(HttpServletRequest request) {
        // 1. 세션 확인
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("sellerUid") == null) {
            return ResponseEntity.ok().build(); // 로그인 안됨 -> 빈 응답
        }

        // 2. 세션에서 sellerUid 가져오기 (int 형변환)
        int sellerUid;
        try {
            sellerUid = Integer.parseInt(String.valueOf(session.getAttribute("sellerUid")));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid Seller UID");
        }

        // 3. MyBatis Repository 호출 (가장 최신 주문 1건)
        ReservationResponse latestOrder = reservationRepository.selectLatestReservationBySeller(sellerUid);

        // 4. 결과 반환
        if (latestOrder != null) {
            // 프론트엔드(OrderNotification.jsx)가 원하는 형태(id, productName)로 변환해서 전송
            return ResponseEntity.ok(Map.of(
                    "id", latestOrder.getReservationId(),
                    "productName", latestOrder.getProductName()
            ));
        } else {
            // 주문 내역이 없으면 빈 응답
            return ResponseEntity.ok().build();
        }
    }
}