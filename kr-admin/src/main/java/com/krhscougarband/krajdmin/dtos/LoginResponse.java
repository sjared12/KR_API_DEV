package com.krhscougarband.krajdmin.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String email;
    private String firstName;
    private String lastName;
}
