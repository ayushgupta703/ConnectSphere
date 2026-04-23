package com.connectsphere.searchservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "hashtags",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "tag")
        },
        indexes = {
                @Index(name = "idx_tag", columnList = "tag"),
                @Index(name = "idx_post_count", columnList = "postCount")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String tag;

    @Column(nullable = false)
    private Long postCount = 0L;

    @Column(nullable = false)
    private LocalDateTime lastUsedAt;

    @PrePersist
    protected void onCreate() {
        this.lastUsedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUsedAt = LocalDateTime.now();
    }
}