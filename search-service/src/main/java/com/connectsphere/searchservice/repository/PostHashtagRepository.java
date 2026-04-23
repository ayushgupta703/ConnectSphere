package com.connectsphere.searchservice.repository;

import com.connectsphere.searchservice.entity.PostHashtag;
import com.connectsphere.searchservice.entity.Hashtag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostHashtagRepository extends JpaRepository<PostHashtag, Long> {

    // ✅ Get all mappings for a post
    List<PostHashtag> findByPostId(String postId);

    // ✅ Get all mappings for a hashtag
    List<PostHashtag> findByHashtag(Hashtag hashtag);

    // ✅ Check if mapping already exists (avoid duplicates)
    Optional<PostHashtag> findByPostIdAndHashtag(String postId, Hashtag hashtag);

    // ✅ Get only postIds for a hashtag (important optimization)
    List<PostHashtag> findByHashtagId(Long hashtagId);

    // ✅ Delete mappings when post is deleted (future use)
    void deleteByPostId(String postId);
}