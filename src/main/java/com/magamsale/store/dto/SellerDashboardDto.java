package com.magamsale.store.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerDashboardDto {
    // 1. 판매 금액 (구매확정 기준)
    private long totalSalesAmount;

    // 2. 상품 현황 카운트
    private int totalProductCount;    // 전체 등록
    private int sellingProductCount;  // 판매중
    private int soldProductCount;     // 판매완료
    private int closedProductCount;   // 마감/종료
}