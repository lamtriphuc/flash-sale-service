package com.lamtriphuc.backend.tenant.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    private String companyName;
    private String email;
    private String password;
}