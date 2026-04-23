package com.connectsphere.searchservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "post_hashtags",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"post_id", "hashtag_id"})
        },
        indexes = {
                @Index(name = "idx_post_id", columnList = "post_id"),
                @Index(name = "idx_hashtag_id", columnList = "hashtag_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostHashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private String postId;   // UUID from Post Service

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hashtag_id", nullable = false)
    private Hashtag hashtag;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}