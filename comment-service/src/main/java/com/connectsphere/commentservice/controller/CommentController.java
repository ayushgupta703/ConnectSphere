package com.connectsphere.commentservice.controller;

import com.connectsphere.commentservice.dto.CommentRequestDto;
import com.connectsphere.commentservice.dto.CommentResponseDto;
import com.connectsphere.commentservice.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // ✅ ADD COMMENT
    @PostMapping("/{postId}")
    public ResponseEntity<CommentResponseDto> addComment(
            @PathVariable UUID postId,
            @Valid @RequestBody CommentRequestDto request,
            @RequestAttribute("userId") UUID userId,
            @RequestHeader("Authorization") String token
    ) {
        return ResponseEntity.ok(
                commentService.addComment(userId, postId, request.getContent(), request.getParentCommentId(), token)
        );
    }

    // ✅ DELETE COMMENT (SOFT DELETE)
    @DeleteMapping("/{commentId}")
    public ResponseEntity<String> deleteComment(
            @PathVariable UUID commentId,
            @RequestAttribute("userId") UUID userId,
            @RequestHeader("Authorization") String token
    ) {
        commentService.deleteComment(commentId, userId, token);
        return ResponseEntity.ok("Comment deleted");
    }

    // ✅ GET COMMENTS (TOP LEVEL)
    @GetMapping("/{postId}")
    public ResponseEntity<Page<CommentResponseDto>> getComments(
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(commentService.getComments(postId, page, size));
    }

    // ✅ GET REPLIES
    @GetMapping("/replies/{commentId}")
    public ResponseEntity<List<CommentResponseDto>> getReplies(@PathVariable UUID commentId) {
        return ResponseEntity.ok(commentService.getReplies(commentId));
    }

    // ✅ COUNT COMMENTS
    @GetMapping("/{postId}/count")
    public ResponseEntity<Long> countComments(@PathVariable UUID postId) {
        return ResponseEntity.ok(commentService.countComments(postId));
    }

    // 🔥 NEW APIs (IMPORTANT)

    // ✅ UPDATE COMMENT
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponseDto> updateComment(
            @PathVariable UUID commentId,
            @RequestBody Map<String, String> body,
            @RequestAttribute("userId") UUID userId
    ) {
        return ResponseEntity.ok(
                commentService.updateComment(commentId, userId, body.get("content"))
        );
    }

    // ✅ GET COMMENT BY ID
    @GetMapping("/single/{commentId}")
    public ResponseEntity<CommentResponseDto> getComment(@PathVariable UUID commentId) {
        return ResponseEntity.ok(commentService.getCommentById(commentId));
    }

    // ✅ GET COMMENTS BY USER
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CommentResponseDto>> getByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(commentService.getCommentsByUser(userId));
    }

    // ✅ LIKE COMMENT
    @PostMapping("/{commentId}/like")
    public ResponseEntity<Void> like(@PathVariable UUID commentId) {
        commentService.likeComment(commentId);
        return ResponseEntity.ok().build();
    }

    // ✅ UNLIKE COMMENT
    @PostMapping("/{commentId}/unlike")
    public ResponseEntity<Void> unlike(@PathVariable UUID commentId) {
        commentService.unlikeComment(commentId);
        return ResponseEntity.ok().build();
    }
}