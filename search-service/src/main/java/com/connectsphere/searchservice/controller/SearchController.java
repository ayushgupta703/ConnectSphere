package com.connectsphere.searchservice.controller;

import com.connectsphere.searchservice.dto.IndexRequestDTO;
import com.connectsphere.searchservice.service.SearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    // ✅ INDEX POST (Feign-friendly + clean DTO)
    @PostMapping("/index")
    public ResponseEntity<String> indexPost(
            @Valid @RequestBody IndexRequestDTO request
    ) {
        searchService.indexPost(request.getPostId(), request.getContent());
        return ResponseEntity.ok("Post indexed successfully");
    }

    // ⚠️ Placeholder for keyword search (future: Elasticsearch)
    @GetMapping("/posts")
    public ResponseEntity<String> searchPosts(@RequestParam String query) {
        return ResponseEntity.ok("Keyword search not implemented yet for: " + query);
    }
}