package com.magamsale.store.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ProductCreateDTO {

    @NotNull(message = "sellerUid는 필수값입니다.")
    @Min(value = 1, message = "sellerUid는 1 이상이어야 합니다.")
    private int sellerUid;

    @NotBlank(message = "상품명은 필수입니다.")
    private String name;

    @NotNull(message = "originalPrice는 필수입니다.")
    private int originalPrice;

    @NotNull(message = "salePrice는 필수입니다.")
    private int salePrice;

    @NotNull(message = "deadlineTime은 필수입니다.")
    private String deadlineTimeStr;  // "19:00"
    private LocalDateTime deadlineTime;

    private String notice;
    private String imageUrl;

    // 📌 위치정보도 여기에서 받아야 함 (폼에서 넣을 거면)
    private Double storeLat;
    private Double storeLng;

    private Integer saleQuantity;
    private String address;

//    private String


}