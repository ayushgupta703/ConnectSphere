package com.connectsphere.mediaservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "story_views", uniqueConstraints = {
    @UniqueConstraint(name = "unique_story_view", columnNames = {"story_id", "viewer_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryView {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "story_id", nullable = false)
    private UUID storyId;

    @Column(name = "viewer_id", nullable = false)
    private UUID viewerId;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;
}
