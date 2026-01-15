package com.magamsale.store.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class SellerSignupDTO {
    private String id;
    private String pw;
    private String phoneNumber;
    private String storeName;
    private String businessUid;
    private String address;
    private Double storeLat;
    private Double storeLng;

}