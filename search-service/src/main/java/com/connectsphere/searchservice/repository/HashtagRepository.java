package com.connectsphere.searchservice.repository;

import com.connectsphere.searchservice.entity.Hashtag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface HashtagRepository extends JpaRepository<Hashtag, Long> {

    // ✅ Find by tag (case-insensitive)
    Optional<Hashtag> findByTagIgnoreCase(String tag);

    // ✅ Search hashtags (for autocomplete)
    List<Hashtag> findByTagContainingIgnoreCase(String keyword);

    // ✅ Trending hashtags (sorted by postCount DESC)
    @Query("SELECT h FROM Hashtag h ORDER BY h.postCount DESC")
    List<Hashtag> findTrendingHashtags(Pageable pageable);
}