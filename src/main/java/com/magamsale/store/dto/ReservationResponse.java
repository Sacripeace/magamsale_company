package com.magamsale.store.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class ReservationResponse {
    private int reservationId;
    private String status;
    private String createdAt;

    private LocalTime reservationTime;

    private int quantity;
    private int totalPrice;
    private String requestMessage;

    // 상품 정보
    private int productId;
    private String productName;
    private String imageUrl;

    // 구매자 정보
    private String buyerName;
    private String buyerPhone;

    // ✅ [추가] 이 3개가 있어야 XML에서 조회한 상점 정보를 받을 수 있습니다!
    private String storeName;     // 상호명
    private String storeAddress;  // 가게 주소
    private String storePhone;    // 가게 연락처
}