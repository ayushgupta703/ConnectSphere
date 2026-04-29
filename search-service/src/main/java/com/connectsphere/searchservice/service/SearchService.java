package com.connectsphere.searchservice.service;

import com.connectsphere.searchservice.dto.UserSearchResponse;
import com.connectsphere.searchservice.entity.Hashtag;

import java.util.List;

public interface SearchService {

    // 🔹 Index hashtags for a post
    void indexPost(String postId, String content);

    // 🔹 Remove index when post is deleted (future-safe)
    void removePostIndex(String postId);

    // 🔹 Get posts by hashtag
    List<String> getPostsByHashtag(String tag);

    // 🔹 Get trending hashtags
    List<Hashtag> getTrendingHashtags(int limit);

    // 🔹 Search hashtags (autocomplete)
    List<Hashtag> searchHashtags(String keyword);

    // 🔹 Get hashtags of a post
    List<String> getHashtagsForPost(String postId);

    // 🔹 Search user by username
    Object searchUserByUsername(String username);

    // 🔹 Search users by name
    List<Object> searchUsersByName(String name);

    // 🔹 Smart unified user search
    List<UserSearchResponse> searchUsers(String query);

    // 🔹 Search posts by hashtag (full objects)
    List<Object> searchPostsByHashtag(String tag);
}