package com.magamsale.store.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FindPwRequest {
    private String businessUid;   // 사업자번호
    private String username;      // 아이디
    private String phoneNumber;   // 연락처
}