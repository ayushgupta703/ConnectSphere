package com.connectsphere.postservice.service;

import com.connectsphere.postservice.client.AuthClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Centralizes all user-deletion validation logic for the post-service.
 *
 * <p>Uses a single batch Feign call to auth-service per logical request,
 * eliminating the N+1 pattern. On any auth-service failure, the helper
 * degrades gracefully by returning an empty set (i.e. assuming no users
 * are deleted) so that feeds remain usable even when auth-service is down.
 */
@Slf4j
@Component
@RequestScope
@RequiredArgsConstructor
public class UserValidationHelper {

    private final AuthClient authClient;

    @Value("${internal.service.secret}")
    private String internalSecret;

    // Cache to prevent redundant calls to auth-service within the same request
    private final Map<UUID, Boolean> userDeletionCache = new HashMap<>();

    /**
     * Given a set of user IDs, returns the subset that are soft-deleted.
     *
     * <p>If the auth-service call fails for any reason, returns an empty
     * set (fail-open / graceful degradation) so posts are still returned
     * rather than hiding all content.
     *
     * @param userIds the user IDs to check (must not be null)
     * @return set of deleted user IDs; empty on failure or empty input
     */
    public Set<UUID> getDeletedUserIds(Set<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptySet();
        }

        // 1. Identify which users are not yet cached
        Set<UUID> uncachedIds = userIds.stream()
                .filter(id -> !userDeletionCache.containsKey(id))
                .collect(Collectors.toSet());

        // 2. Fetch uncached users from auth-service
        if (!uncachedIds.isEmpty()) {
            try {
                Set<UUID> newlyDeletedIds = authClient.getBatchDeletedUsers(uncachedIds, internalSecret);
                if (newlyDeletedIds == null) {
                    newlyDeletedIds = Collections.emptySet();
                }
                
                // Cache the results
                for (UUID id : uncachedIds) {
                    userDeletionCache.put(id, newlyDeletedIds.contains(id));
                }
            } catch (Exception e) {
                log.error("Batch user-deletion check failed — auth-service may be unavailable. " +
                          "Degrading gracefully: assuming no users are deleted. Error: {}", e.getMessage());
                // Cache as NOT deleted (graceful degradation) to prevent repeatedly trying if it's down
                for (UUID id : uncachedIds) {
                    userDeletionCache.put(id, false);
                }
            }
        }

        // 3. Return all deleted users from cache that are in the requested userIds set
        return userIds.stream()
                .filter(id -> Boolean.TRUE.equals(userDeletionCache.get(id)))
                .collect(Collectors.toSet());
    }

    /**
     * Convenience method to check whether a single user is soft-deleted.
     * Prefer {@link #getDeletedUserIds(Set)} when checking multiple users at once.
     */
    public boolean isOwnerDeleted(UUID userId) {
        return getDeletedUserIds(Set.of(userId)).contains(userId);
    }
}
