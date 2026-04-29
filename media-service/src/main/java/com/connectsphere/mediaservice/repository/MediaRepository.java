package com.connectsphere.mediaservice.repository;

import com.connectsphere.mediaservice.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaRepository extends JpaRepository<Media, java.util.UUID> {

    // 🔹 Get media linked to a post (ONLY active)
    List<Media> findByLinkedPostIdAndIsDeletedFalse(java.util.UUID postId);

    // 🔹 Get media uploaded by user
    List<Media> findByUploaderIdAndIsDeletedFalse(java.util.UUID uploaderId);

    // 🔹 Get media by type
    List<Media> findByMediaTypeAndIsDeletedFalse(com.connectsphere.mediaservice.entity.MediaType mediaType);

}