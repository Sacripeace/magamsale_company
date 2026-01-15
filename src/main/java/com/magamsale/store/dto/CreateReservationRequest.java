package com.magamsale.store.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalTime;

@Data
public class CreateReservationRequest {
    private int productId;
    private int quantity;

    // [수정됨] 프론트에서 "18:00" 문자열로 보내도 LocalTime으로 자동 변환되도록 설정
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime reservationTime;

    private String requestMessage;
}