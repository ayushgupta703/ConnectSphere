package com.connectsphere.mediaservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "media",
        indexes = {
                @Index(name = "idx_post_id", columnList = "linkedPostId"),
                @Index(name = "idx_uploader_id", columnList = "uploaderId")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long uploaderId;

    @Column(nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    private MediaType mediaType;

    private Long sizeKb;

    private String mimeType;

    private Long linkedPostId;

    private LocalDateTime uploadedAt;

    private boolean isDeleted = false;

    @PrePersist
    public void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }
}