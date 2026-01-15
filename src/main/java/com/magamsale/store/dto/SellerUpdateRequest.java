package com.magamsale.store.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerUpdateRequest {

    private String storeName;      // 상점명
    private String ownerName;      // 대표자명 (없으면 null)
    @JsonProperty("phone")
    private String phoneNumber;          // 전화번호
    private String businessNumber; // 사업자번호
    private String address;        // 주소

    private String storeOpen;      // 오픈 시간
    private String storeClose;     // 마감 시간

    private Double storeLat;
    private Double storeLng;

    private String bankName;       // (없으면 null)
    private String accountNumber;  // (없으면 null)
    private String accountHolder;  // (없으면 null)
}