package com.connectsphere.searchservice.controller;

import com.connectsphere.searchservice.dto.HashtagListResponseDTO;
import com.connectsphere.searchservice.dto.HashtagResponseDTO;
import com.connectsphere.searchservice.dto.PostIdsResponseDTO;
import com.connectsphere.searchservice.entity.Hashtag;
import com.connectsphere.searchservice.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/hashtags")
@RequiredArgsConstructor
public class HashtagController {

    private final SearchService searchService;

    // ✅ GET POSTS BY HASHTAG
    @GetMapping("/{tag}/posts")
    public ResponseEntity<PostIdsResponseDTO> getPostsByHashtag(@PathVariable String tag) {

        List<String> postIds = searchService.getPostsByHashtag(tag);

        return ResponseEntity.ok(
                PostIdsResponseDTO.builder()
                        .hashtag(tag)
                        .postIds(postIds)
                        .build()
        );
    }

    // ✅ TRENDING HASHTAGS
    @GetMapping("/trending")
    public ResponseEntity<List<HashtagResponseDTO>> getTrendingHashtags(
            @RequestParam(defaultValue = "10") int limit
    ) {

        List<HashtagResponseDTO> response = searchService.getTrendingHashtags(limit)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // ✅ SEARCH HASHTAGS (autocomplete)
    @GetMapping("/search")
    public ResponseEntity<List<HashtagResponseDTO>> searchHashtags(
            @RequestParam String keyword
    ) {

        List<HashtagResponseDTO> response = searchService.searchHashtags(keyword)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // ✅ GET HASHTAGS FOR A POST
    @GetMapping("/post/{postId}")
    public ResponseEntity<HashtagListResponseDTO> getHashtagsForPost(
            @PathVariable String postId
    ) {

        List<String> hashtags = searchService.getHashtagsForPost(postId);

        return ResponseEntity.ok(
                HashtagListResponseDTO.builder()
                        .postId(postId)
                        .hashtags(hashtags)
                        .build()
        );
    }

    // 🔹 Mapper Method
    private HashtagResponseDTO mapToDTO(Hashtag hashtag) {
        return HashtagResponseDTO.builder()
                .tag(hashtag.getTag())
                .postCount(hashtag.getPostCount())
                .build();
    }
}