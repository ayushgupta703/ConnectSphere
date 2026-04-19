package com.connectsphere.postservice.entity;

import com.connectsphere.postservice.enums.PostVisibility;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ElementCollection
    @CollectionTable(name = "post_media_urls", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "media_url")
    private List<String> mediaUrls;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostVisibility visibility;

    @Column(nullable = false)
    private long likesCount = 0;

    @Column(nullable = false)
    private long commentsCount = 0;

    @Column(nullable = false)
    private boolean isDeleted = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Lifecycle Hooks
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}