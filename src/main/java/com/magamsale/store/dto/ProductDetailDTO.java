package com.magamsale.store.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductDetailDTO {

    private int productId;
    private int sellerUid;

    private String productName;
    private int originalPrice;
    private int salePrice;
    private String deadlineTime;

    private String notice;
    private String imageUrl;

    private Double storeLat;
    private Double storeLng;

    // 👇 seller_tb에서 JOIN
    private String storeName;
    private String address;
    private int saleQuantity;
    private String status;

    // product[로그인안해도 정보로는]
    private String phoneNumber;
    private String storeOpen;
    private String storeClose;

    private String bankName;
    private String accountNumber;
    private String accountHolder;
}