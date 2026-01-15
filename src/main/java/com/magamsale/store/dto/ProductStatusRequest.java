package com.magamsale.store.dto;

import com.magamsale.store.entity.ProductStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductStatusRequest {
    private ProductStatus status; // ACTIVE / HIDDEN
}