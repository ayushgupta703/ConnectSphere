package com.connectsphere.searchservice.client;

import com.connectsphere.searchservice.config.FeignConfig;
import com.connectsphere.searchservice.dto.AuthUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "auth-service", url = "http://localhost:8080", configuration = FeignConfig.class)
public interface AuthClient {

    @GetMapping("/api/v1/auth/users/search/username")
    AuthUserResponse searchUserByUsername(@RequestParam("value") String username);

    @GetMapping("/api/v1/auth/users/search/name")
    List<AuthUserResponse> searchUsersByName(@RequestParam("value") String name);

    @GetMapping("/api/v1/auth/users/search/prefix")
    List<AuthUserResponse> searchUsersByUsernamePrefix(@RequestParam("value") String value);
}
