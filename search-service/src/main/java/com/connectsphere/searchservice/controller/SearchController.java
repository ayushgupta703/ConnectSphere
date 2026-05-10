package com.connectsphere.searchservice.controller;

import com.connectsphere.searchservice.dto.UserSearchResponse;
import com.connectsphere.searchservice.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    // ✅ SMART UNIFIED USER SEARCH
    @GetMapping("/users")
    public ResponseEntity<List<UserSearchResponse>> searchUsers(@RequestParam String query) {
        return ResponseEntity.ok(searchService.searchUsers(query));
    }

    // ✅ SEARCH USER BY USERNAME (Unique) - OLD ENDPOINT
    @GetMapping("/users/username")
    public ResponseEntity<?> searchUserByUsername(@RequestParam String value) {
        if (value == null || value.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Username value cannot be empty");
        }
        Object result = searchService.searchUserByUsername(value.trim());
        return ResponseEntity.ok(result);
    }

    // ✅ SEARCH USERS BY FULL NAME (Non-unique) - OLD ENDPOINT
    @GetMapping("/users/name")
    public ResponseEntity<?> searchUsersByName(@RequestParam String value) {
        if (value == null || value.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Name value cannot be empty");
        }
        Object result = searchService.searchUsersByName(value.trim());
        return ResponseEntity.ok(result);
    }

    // ✅ SEARCH HASHTAGS (Return posts)
    @GetMapping("/hashtags")
    public ResponseEntity<?> searchHashtags(@RequestParam String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Tag value cannot be empty");
        }
        Object result = searchService.searchPostsByHashtag(tag.trim());
        return ResponseEntity.ok(result);
    }
}