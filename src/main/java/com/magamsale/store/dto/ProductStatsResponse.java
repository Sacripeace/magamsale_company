package com.magamsale.store.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductStatsResponse {
    private long total;
    private long sold;
    private long selling;
    private long closed;
}