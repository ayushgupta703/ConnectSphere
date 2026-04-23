package com.connectsphere.mediaservice.repository;

import com.connectsphere.mediaservice.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaRepository extends JpaRepository<Media, Long> {

    // 🔹 Get media linked to a post (ONLY active)
    List<Media> findByLinkedPostIdAndIsDeletedFalse(Long postId);

    // 🔹 Get media uploaded by user
    List<Media> findByUploaderIdAndIsDeletedFalse(Long uploaderId);

}