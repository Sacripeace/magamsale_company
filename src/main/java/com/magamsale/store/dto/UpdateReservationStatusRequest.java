package com.magamsale.store.dto;

import lombok.Data;

@Data
public class UpdateReservationStatusRequest {
    private String status; // "COMPLETED" or "CANCELLED"
}