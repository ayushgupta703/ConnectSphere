package com.connectsphere.likeservice.controller;

import com.connectsphere.likeservice.dto.ReactionRequest;
import com.connectsphere.likeservice.dto.ReactionSummaryResponse;
import com.connectsphere.likeservice.service.LikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reactions")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping
    public ResponseEntity<String> react(
            @Valid @RequestBody ReactionRequest request,
            @RequestAttribute("userId") UUID userId
    ) {
        likeService.react(request, userId);
        return ResponseEntity.ok("Reaction added/updated");
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<String> removeReaction(
            @PathVariable UUID postId,
            @RequestAttribute("userId") UUID userId
    ) {
        likeService.removeReaction(postId, userId);
        return ResponseEntity.ok("Reaction removed");
    }

    @GetMapping("/{postId}/count")
    public ResponseEntity<Long> count(@PathVariable UUID postId) {
        return ResponseEntity.ok(likeService.getTotalReactions(postId));
    }

    @GetMapping("/{postId}/summary")
    public ResponseEntity<ReactionSummaryResponse> summary(@PathVariable UUID postId) {
        return ResponseEntity.ok(likeService.getReactionSummary(postId));
    }
}