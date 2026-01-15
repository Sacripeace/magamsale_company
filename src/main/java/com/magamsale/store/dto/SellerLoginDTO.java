package com.magamsale.store.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerLoginDTO {

    @JsonAlias({"businessUid", "businessId", "business_number"})
    private String businessNumber;

    @JsonAlias({"id", "userId"})
    private String username;

    @JsonAlias({"pw"})
    private String password;
}