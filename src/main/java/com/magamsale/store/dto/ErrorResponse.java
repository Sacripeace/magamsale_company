package com.magamsale.store.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorResponse {

    private final int status;
    private final String message;

    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }

}