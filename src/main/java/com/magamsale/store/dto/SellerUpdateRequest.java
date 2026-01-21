package com.magamsale.store.dto;

// import com.fasterxml.jackson.annotation.JsonProperty; // 🚨 이거 필요 없어졌으니 지우셔도 됩니다.
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerUpdateRequest {

    private String storeName;
    private String ownerName;

    private String phoneNumber;

    private String businessNumber;
    private String address;

    private String storeOpen;
    private String storeClose;

    private Double storeLat;
    private Double storeLng;

    private String bankName;
    private String accountNumber;
    private String accountHolder;
}