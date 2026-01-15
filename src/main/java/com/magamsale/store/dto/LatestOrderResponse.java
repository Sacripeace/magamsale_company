package com.magamsale.dto; // 패키지명은 본인 프로젝트에 맞게 수정

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LatestOrderResponse {
    private Long id;           // 주문 번호 (기준점)
    private String productName; // 알림창에 띄울 상품명
}