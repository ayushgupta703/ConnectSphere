package com.connectsphere.authservice.dto;

import lombok.Data;


@Data
public class UpdateProfileRequest {

    private String fullName;
    private String username;
    private String email;
    private String bio;
    private String profilePicUrl;
}

