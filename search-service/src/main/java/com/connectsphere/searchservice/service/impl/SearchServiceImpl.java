package com.connectsphere.searchservice.service.impl;

import com.connectsphere.searchservice.dto.AuthUserResponse;
import com.connectsphere.searchservice.dto.MatchType;
import com.connectsphere.searchservice.dto.UserSearchResponse;
import com.connectsphere.searchservice.entity.Hashtag;
import com.connectsphere.searchservice.entity.PostHashtag;
import com.connectsphere.searchservice.repository.HashtagRepository;
import com.connectsphere.searchservice.repository.PostHashtagRepository;
import com.connectsphere.searchservice.service.SearchService;
import com.connectsphere.searchservice.client.AuthClient;
import com.connectsphere.searchservice.client.PostClient;
import com.connectsphere.searchservice.util.HashtagExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {

    private final HashtagRepository hashtagRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final HashtagExtractor hashtagExtractor;
    private final AuthClient authClient;
    private final PostClient postClient;


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
        String cleanTag = tag.replace("#", "").trim().toLowerCase();

        // 🔹 Fetch ALL matching hashtags (partial search)
        List<Hashtag> hashtags = hashtagRepository.findByTagContainingIgnoreCase(cleanTag);

        if (hashtags.isEmpty()) {
            return Collections.emptyList();
        }

        // 🔹 Combine all postIds from all matching hashtags and remove duplicates
        return hashtags.stream()
                .flatMap(h -> postHashtagRepository.findByHashtag(h).stream())
                .map(PostHashtag::getPostId)
                .distinct()
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

    // ✅ SEARCH USER BY USERNAME
    @Override
    public Object searchUserByUsername(String username) {
        try {
            return authClient.searchUserByUsername(username);
        } catch (Exception e) {
            return null; // Return null if not found
        }
    }

    // ✅ SEARCH USERS BY NAME
    @Override
    public List<Object> searchUsersByName(String name) {
        try {
            return authClient.searchUsersByName(name).stream().map(u -> (Object)u).collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // ✅ SMART UNIFIED SEARCH
    @Override
    public List<UserSearchResponse> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedQuery = query.trim().toLowerCase();

        if(normalizedQuery.startsWith("@")) {
            normalizedQuery = normalizedQuery.substring(1);
        }

        Map<UUID, UserSearchResponse> resultsMap = new LinkedHashMap<>();

        try {
            // 1. Exact username match
            AuthUserResponse exactMatch = authClient.searchUserByUsername(normalizedQuery);
            if (exactMatch != null) {
                resultsMap.put(exactMatch.getId(), mapToUserSearchResponse(exactMatch, MatchType.USERNAME_EXACT));
            }
        } catch (Exception e) {
//            log.error("Error fetching exact username match: {}", e.getMessage());
            
        }

        try {
            // 2. Prefix username match
            List<AuthUserResponse> prefixMatches = authClient.searchUsersByUsernamePrefix(normalizedQuery);
            for (AuthUserResponse user : prefixMatches) {
                resultsMap.putIfAbsent(user.getId(), mapToUserSearchResponse(user, MatchType.USERNAME_PREFIX));
            }
        } catch (Exception e) {
            log.error("Error fetching username prefix matches: {}", e.getMessage());
        }

        try {
            // 3. Name contains match
            List<AuthUserResponse> nameMatches = authClient.searchUsersByName(normalizedQuery);
            for (AuthUserResponse user : nameMatches) {
                resultsMap.putIfAbsent(user.getId(), mapToUserSearchResponse(user, MatchType.NAME_CONTAINS));
            }
        } catch (Exception e) {
            log.error("Error fetching name contains matches: {}", e.getMessage());
        }

        return new ArrayList<>(resultsMap.values());
    }

    private UserSearchResponse mapToUserSearchResponse(AuthUserResponse authUser, MatchType matchType) {
        return UserSearchResponse.builder()
                .id(authUser.getId())
                .username(authUser.getUsername())
                .fullName(authUser.getFullName())
                .matchType(matchType)
                .build();
    }

    // ✅ SEARCH POSTS BY HASHTAG
    @Override
    public List<Object> searchPostsByHashtag(String tag) {
        try {
            // Remove '#' if present
            String cleanTag = tag.replace("#", "").trim().toLowerCase();

            List<String> postIds = getPostsByHashtag(cleanTag);

            if (postIds == null || postIds.isEmpty()) {
                return Collections.emptyList();
            }

            List<UUID> uuidList = postIds.stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toList());

            return postClient.getPostsByIds(uuidList);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}