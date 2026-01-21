package com.magamsale.store.dto;

import lombok.*;

@Getter
@Setter
public class SellerInfoResponse {
    private int sellerUid;        // 판매자 UID(기본키)
    private String storeName;      // 상점명
    private String ownerName;      // 대표자명 (없으면 null)
    private String phoneNumber;    // 전화번호
    private String businessNumber; // 사업자번호
    private String address;        // 주소

    private String storeOpen;      // 오픈 시간
    private String storeClose;     // 마감 시간

    private String bankName;       // (없으면 null)
    private String accountNumber;  // (없으면 null)
    private String accountHolder;  // (없으면 null)
}