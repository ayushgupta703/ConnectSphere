package com.connectsphere.mediaservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stories",
        indexes = {
                @Index(name = "idx_author_id", columnList = "authorId"),
                @Index(name = "idx_expiry", columnList = "expiresAt")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long authorId;

    @Column(nullable = false)
    private String mediaUrl;

    private String caption;

    @Enumerated(EnumType.STRING)
    private MediaType mediaType;

    private Long viewsCount = 0L;

    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    private boolean isActive = true;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.expiresAt = createdAt.plusHours(24);
        this.isActive = true;
    }
}