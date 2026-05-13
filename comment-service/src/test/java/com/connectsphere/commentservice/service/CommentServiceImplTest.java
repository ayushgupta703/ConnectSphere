package com.connectsphere.commentservice.service;

import com.connectsphere.commentservice.client.PostClient;
import com.connectsphere.commentservice.dto.CommentResponseDto;
import com.connectsphere.commentservice.entity.Comment;
import com.connectsphere.commentservice.event.CommentNotificationEvent;
import com.connectsphere.commentservice.exception.BadRequestException;
import com.connectsphere.commentservice.exception.ResourceNotFoundException;
import com.connectsphere.commentservice.exception.UnauthorizedException;
import com.connectsphere.commentservice.producer.NotificationEventProducer;
import com.connectsphere.commentservice.repository.CommentRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentServiceImpl Unit Tests")
class CommentServiceImplTest {

    @Mock private CommentRepository commentRepository;
    @Mock private PostClient postClient;
    @Mock private NotificationEventProducer notificationEventProducer;

    @InjectMocks private CommentServiceImpl commentService;

    private UUID userId;
    private UUID postId;
    private UUID commentId;

    @BeforeEach
    void setUp() {
        userId    = UUID.randomUUID();
        postId    = UUID.randomUUID();
        commentId = UUID.randomUUID();
    }

    // ── helper ────────────────────────────────────────────────────────────
    private Comment buildComment(UUID id, UUID uid, UUID pid, UUID parentId) {
        Comment c = new Comment();
        c.setId(id);
        c.setUserId(uid);
        c.setPostId(pid);
        c.setParentCommentId(parentId);
        c.setContent("Test comment");
        c.setLikesCount(0);
        c.setDeleted(false);
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        return c;
    }

    // ── addComment ────────────────────────────────────────────────────────

    @Test
    @DisplayName("addComment: saves top-level comment and notifies post owner when different from commenter")
    void addComment_topLevel_notifiesPostOwner() {
        UUID postOwnerId = UUID.randomUUID();
        Comment saved = buildComment(commentId, userId, postId, null);

        when(commentRepository.save(any())).thenReturn(saved);
        when(commentRepository.countByParentCommentIdAndDeletedFalse(commentId)).thenReturn(0L);
        when(postClient.getPostOwner(postId, "tok")).thenReturn(postOwnerId);
        doNothing().when(postClient).incrementComments(postId, "tok");

        CommentResponseDto res = commentService.addComment(userId, postId, "Hello!", null, "tok");

        assertNotNull(res);
        assertEquals(userId, res.getUserId());
        verify(notificationEventProducer).publishNotificationEvent(any());
    }

    @Test
    @DisplayName("addComment: does NOT notify when commenter is the post owner")
    void addComment_topLevel_noNotifyForSameOwner() {
        Comment saved = buildComment(commentId, userId, postId, null);

        when(commentRepository.save(any())).thenReturn(saved);
        when(commentRepository.countByParentCommentIdAndDeletedFalse(commentId)).thenReturn(0L);
        when(postClient.getPostOwner(postId, "tok")).thenReturn(userId);
        doNothing().when(postClient).incrementComments(postId, "tok");

        commentService.addComment(userId, postId, "Hello!", null, "tok");

        verify(notificationEventProducer, never()).publishNotificationEvent(any());
    }

    @Test
    @DisplayName("addComment: reply saves and notifies parent comment owner with type REPLY")
    void addComment_reply_notifiesCommentOwner() {
        UUID parentId = UUID.randomUUID();
        UUID parentOwnerId = UUID.randomUUID();
        Comment parent = buildComment(parentId, parentOwnerId, postId, null);
        Comment saved  = buildComment(commentId, userId, postId, parentId);

        when(commentRepository.findByIdAndDeletedFalse(parentId)).thenReturn(Optional.of(parent));
        when(commentRepository.save(any())).thenReturn(saved);
        when(commentRepository.countByParentCommentIdAndDeletedFalse(commentId)).thenReturn(0L);
        doNothing().when(postClient).incrementComments(postId, "tok");

        commentService.addComment(userId, postId, "Reply!", parentId, "tok");

        verify(notificationEventProducer).publishNotificationEvent(
                argThat((CommentNotificationEvent e) -> "REPLY".equals(e.getType())));
    }

    @Test
    @DisplayName("addComment: reply does NOT notify when replying to own comment")
    void addComment_reply_noNotifyForSameUser() {
        UUID parentId = UUID.randomUUID();
        Comment parent = buildComment(parentId, userId, postId, null);
        Comment saved  = buildComment(commentId, userId, postId, parentId);

        when(commentRepository.findByIdAndDeletedFalse(parentId)).thenReturn(Optional.of(parent));
        when(commentRepository.save(any())).thenReturn(saved);
        when(commentRepository.countByParentCommentIdAndDeletedFalse(commentId)).thenReturn(0L);
        doNothing().when(postClient).incrementComments(postId, "tok");

        commentService.addComment(userId, postId, "Self reply", parentId, "tok");

        verify(notificationEventProducer, never()).publishNotificationEvent(any());
    }

    @Test
    @DisplayName("addComment: throws BadRequestException when parent is already a reply (3-level blocked)")
    void addComment_parentAlreadyReply_throws() {
        UUID parentId = UUID.randomUUID();
        Comment parent = buildComment(parentId, UUID.randomUUID(), postId, UUID.randomUUID());
        when(commentRepository.findByIdAndDeletedFalse(parentId)).thenReturn(Optional.of(parent));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> commentService.addComment(userId, postId, "Deep", parentId, "tok"));

        assertEquals("Only 2-level comments allowed", ex.getMessage());
    }

    @Test
    @DisplayName("addComment: throws ResourceNotFoundException when parent belongs to different post")
    void addComment_parentWrongPost_throws() {
        UUID parentId = UUID.randomUUID();
        Comment parent = buildComment(parentId, UUID.randomUUID(), UUID.randomUUID(), null);
        when(commentRepository.findByIdAndDeletedFalse(parentId)).thenReturn(Optional.of(parent));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> commentService.addComment(userId, postId, "Reply", parentId, "tok"));

        assertEquals("Invalid parent comment", ex.getMessage());
    }

    @Test
    @DisplayName("addComment: throws ResourceNotFoundException when parent comment not found")
    void addComment_parentNotFound_throws() {
        UUID parentId = UUID.randomUUID();
        when(commentRepository.findByIdAndDeletedFalse(parentId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> commentService.addComment(userId, postId, "Reply", parentId, "tok"));

        assertEquals("Parent comment not found", ex.getMessage());
    }

    // ── deleteComment ─────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteComment: soft-deletes and decrements post count")
    void deleteComment_success() {
        Comment comment = buildComment(commentId, userId, postId, null);
        when(commentRepository.findByIdAndDeletedFalse(commentId)).thenReturn(Optional.of(comment));
        doNothing().when(postClient).decrementComments(postId, "tok");

        commentService.deleteComment(commentId, userId, "tok");

        assertTrue(comment.isDeleted());
        verify(commentRepository).save(comment);
        verify(postClient).decrementComments(postId, "tok");
    }

    @Test
    @DisplayName("deleteComment: throws UnauthorizedException when not the comment owner")
    void deleteComment_notOwner_throws() {
        Comment comment = buildComment(commentId, userId, postId, null);
        when(commentRepository.findByIdAndDeletedFalse(commentId)).thenReturn(Optional.of(comment));

        assertThrows(UnauthorizedException.class,
                () -> commentService.deleteComment(commentId, UUID.randomUUID(), "tok"));
    }

    @Test
    @DisplayName("deleteComment: throws ResourceNotFoundException when comment not found")
    void deleteComment_notFound_throws() {
        when(commentRepository.findByIdAndDeletedFalse(commentId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> commentService.deleteComment(commentId, userId, "tok"));

        assertEquals("Comment not found", ex.getMessage());
    }

    // ── getComments ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getComments: returns paginated top-level comments with replies count")
    void getComments_success() {
        Comment c = buildComment(commentId, userId, postId, null);
        Pageable pageable = PageRequest.of(0, 10);

        when(commentRepository.findByPostIdAndParentCommentIdIsNullAndDeletedFalse(postId, pageable))
                .thenReturn(new PageImpl<>(List.of(c)));
        when(commentRepository.countByParentCommentIdAndDeletedFalse(commentId)).thenReturn(2L);

        Page<CommentResponseDto> result = commentService.getComments(postId, 0, 10);

        assertEquals(1, result.getContent().size());
        assertEquals(2L, result.getContent().get(0).getRepliesCount());
    }

    // ── getReplies ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getReplies: returns list of replies with correct parentCommentId")
    void getReplies_success() {
        UUID replyId = UUID.randomUUID();
        Comment reply = buildComment(replyId, userId, postId, commentId);

        when(commentRepository.findByParentCommentIdAndDeletedFalse(commentId)).thenReturn(List.of(reply));
        when(commentRepository.countByParentCommentIdAndDeletedFalse(replyId)).thenReturn(0L);

        List<CommentResponseDto> result = commentService.getReplies(commentId);

        assertEquals(1, result.size());
        assertEquals(commentId, result.get(0).getParentCommentId());
    }

    // ── countComments ─────────────────────────────────────────────────────

    @Test
    @DisplayName("countComments: returns count from repository")
    void countComments_returnsCount() {
        when(commentRepository.countByPostIdAndDeletedFalse(postId)).thenReturn(7L);

        assertEquals(7L, commentService.countComments(postId));
    }

    // ── updateComment ─────────────────────────────────────────────────────

    @Test
    @DisplayName("updateComment: updates content and saves")
    void updateComment_success() {
        Comment comment = buildComment(commentId, userId, postId, null);
        when(commentRepository.findByIdAndDeletedFalse(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.save(comment)).thenReturn(comment);
        when(commentRepository.countByParentCommentIdAndDeletedFalse(commentId)).thenReturn(0L);

        commentService.updateComment(commentId, userId, "Updated");

        assertEquals("Updated", comment.getContent());
    }

    @Test
    @DisplayName("updateComment: throws UnauthorizedException when not the owner")
    void updateComment_notOwner_throws() {
        Comment comment = buildComment(commentId, userId, postId, null);
        when(commentRepository.findByIdAndDeletedFalse(commentId)).thenReturn(Optional.of(comment));

        assertThrows(UnauthorizedException.class,
                () -> commentService.updateComment(commentId, UUID.randomUUID(), "New"));
    }

    // ── getCommentById ────────────────────────────────────────────────────

    @Test
    @DisplayName("getCommentById: returns CommentResponseDto for existing comment")
    void getCommentById_success() {
        Comment comment = buildComment(commentId, userId, postId, null);
        when(commentRepository.findByIdAndDeletedFalse(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.countByParentCommentIdAndDeletedFalse(commentId)).thenReturn(0L);

        CommentResponseDto res = commentService.getCommentById(commentId);

        assertEquals(commentId, res.getId());
    }

    @Test
    @DisplayName("getCommentById: throws ResourceNotFoundException when comment not found")
    void getCommentById_notFound_throws() {
        when(commentRepository.findByIdAndDeletedFalse(commentId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> commentService.getCommentById(commentId));

        assertEquals("Comment not found", ex.getMessage());
    }

    // ── getCommentsByUser ─────────────────────────────────────────────────

    @Test
    @DisplayName("getCommentsByUser: returns all comments for the given user")
    void getCommentsByUser_success() {
        Comment comment = buildComment(commentId, userId, postId, null);
        when(commentRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(List.of(comment));
        when(commentRepository.countByParentCommentIdAndDeletedFalse(commentId)).thenReturn(0L);

        List<CommentResponseDto> result = commentService.getCommentsByUser(userId);

        assertEquals(1, result.size());
        assertEquals(userId, result.get(0).getUserId());
    }

    // ── likeComment / unlikeComment ───────────────────────────────────────

    @Test
    @DisplayName("likeComment: increments likes count by 1")
    void likeComment_incrementsCount() {
        Comment comment = buildComment(commentId, userId, postId, null);
        comment.setLikesCount(3);
        when(commentRepository.findByIdAndDeletedFalse(commentId)).thenReturn(Optional.of(comment));

        commentService.likeComment(commentId);

        assertEquals(4, comment.getLikesCount());
        verify(commentRepository).save(comment);
    }

    @Test
    @DisplayName("unlikeComment: decrements likes count by 1")
    void unlikeComment_decrementsCount() {
        Comment comment = buildComment(commentId, userId, postId, null);
        comment.setLikesCount(5);
        when(commentRepository.findByIdAndDeletedFalse(commentId)).thenReturn(Optional.of(comment));

        commentService.unlikeComment(commentId);

        assertEquals(4, comment.getLikesCount());
    }

    @Test
    @DisplayName("unlikeComment: does NOT go below zero")
    void unlikeComment_floorAtZero() {
        Comment comment = buildComment(commentId, userId, postId, null);
        comment.setLikesCount(0);
        when(commentRepository.findByIdAndDeletedFalse(commentId)).thenReturn(Optional.of(comment));

        commentService.unlikeComment(commentId);

        assertEquals(0, comment.getLikesCount());
    }
}
