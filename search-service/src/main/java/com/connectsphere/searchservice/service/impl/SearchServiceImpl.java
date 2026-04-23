package com.connectsphere.searchservice.service.impl;

import com.connectsphere.searchservice.entity.Hashtag;
import com.connectsphere.searchservice.entity.PostHashtag;
import com.connectsphere.searchservice.repository.HashtagRepository;
import com.connectsphere.searchservice.repository.PostHashtagRepository;
import com.connectsphere.searchservice.service.SearchService;
import com.connectsphere.searchservice.util.HashtagExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final HashtagRepository hashtagRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final HashtagExtractor hashtagExtractor;

    // ✅ INDEX POST
    @Override
    @Transactional
    public void indexPost(String postId, String content) {

        // 🔹 Extract hashtags
        Set<String> extractedTags = hashtagExtractor.extract(content);

        for (String tag : extractedTags) {

            // 🔹 Normalize
            String normalizedTag = tag.toLowerCase();

            // 🔹 Find or create hashtag
            Hashtag hashtag = hashtagRepository.findByTagIgnoreCase(normalizedTag)
                    .orElseGet(() -> hashtagRepository.save(
                            Hashtag.builder()
                                    .tag(normalizedTag)
                                    .postCount(0L)
                                    .build()
                    ));

            // 🔹 Check if mapping already exists
            boolean exists = postHashtagRepository
                    .findByPostIdAndHashtag(postId, hashtag)
                    .isPresent();

            if (!exists) {

                // 🔹 Save mapping
                PostHashtag mapping = PostHashtag.builder()
                        .postId(postId)
                        .hashtag(hashtag)
                        .build();

                postHashtagRepository.save(mapping);

                // 🔹 Update count
                hashtag.setPostCount(hashtag.getPostCount() + 1);
                hashtagRepository.save(hashtag);
            }
        }
    }

    // ✅ REMOVE POST INDEX
    @Override
    @Transactional
    public void removePostIndex(String postId) {

        List<PostHashtag> mappings = postHashtagRepository.findByPostId(postId);

        for (PostHashtag mapping : mappings) {
            Hashtag hashtag = mapping.getHashtag();

            // 🔹 Decrement count safely
            if (hashtag.getPostCount() > 0) {
                hashtag.setPostCount(hashtag.getPostCount() - 1);
            }

            hashtagRepository.save(hashtag);
        }

        postHashtagRepository.deleteByPostId(postId);
    }

    // ✅ GET POSTS BY HASHTAG
    @Override
    public List<String> getPostsByHashtag(String tag) {

        Hashtag hashtag = hashtagRepository.findByTagIgnoreCase(tag)
                .orElseThrow(() -> new RuntimeException("Hashtag not found"));

        return postHashtagRepository.findByHashtag(hashtag)
                .stream()
                .map(PostHashtag::getPostId)
                .collect(Collectors.toList());
    }

    // ✅ TRENDING HASHTAGS
    @Override
    public List<Hashtag> getTrendingHashtags(int limit) {
        return hashtagRepository.findTrendingHashtags(PageRequest.of(0, limit));
    }

    // ✅ SEARCH HASHTAGS
    @Override
    public List<Hashtag> searchHashtags(String keyword) {
        return hashtagRepository.findByTagContainingIgnoreCase(keyword);
    }

    // ✅ GET HASHTAGS FOR A POST
    @Override
    public List<String> getHashtagsForPost(String postId) {

        return postHashtagRepository.findByPostId(postId)
                .stream()
                .map(ph -> ph.getHashtag().getTag())
                .collect(Collectors.toList());
    }
}