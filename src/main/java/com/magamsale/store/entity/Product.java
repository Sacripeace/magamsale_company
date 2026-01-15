package com.magamsale.store.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Product {
    private Integer productId;       // 제품 UID
    private Integer sellerUid;       // 판매자 UID
    private String productName;      // 상품명

    private Integer originalPrice;   // 권장 소비자가
    private Integer salePrice;       // 마감세일가
    private String deadlineTime;     // 상품 판매 마감시간
    private String imageUrl;         // 상품 이미지가 있는 폴더 경로
    private String notice;           // 매장에서 안내하는 문구

    private String address;          // 매장 주소
    private String storeName;        // 매장명
    private String createdAt;        // 상품 업로드 날짜
    private Double storeLat;         // 매장 지도위치의 위도
    private Double storeLng;         // 매장 지도위치의 경도
    private int saleQuantity;        // 상품 판매 수량

    private ProductStatus status;           // 상품판매에 대한 ACTIVE / HIDDEN / DELETED (DB VARCHAR 컬럼)


    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getName() {
        return this.getProductName();
    }

    public void setName(String name) {
        this.setProductName(name);
    }
}