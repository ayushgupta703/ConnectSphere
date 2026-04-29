package com.connectsphere.searchservice.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class AuthUserResponse {
    private UUID id;
    private String fullName;
    private String username;
}
