package com.connectsphere.postservice.client;

import com.connectsphere.postservice.dto.request.IndexRequestDTO;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "search-service", url = "http://localhost:8086")
public interface SearchClient {
    @PostMapping("api/v1/search/index")
    void indexPost(
            @Valid @RequestBody IndexRequestDTO request
    );
}
