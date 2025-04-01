package com.ginkgooai.core.identity.service;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ShareCodeService {

    private static final String REDIS_KEY_PREFIX = "share_code:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Generates a guest code for accessing a specific resource.
     *
     * @param resource    The Type of the resource to be accessed
     * @param resourceId  The ID of the resource to be accessed
     * @param userId      The ID of the user
     * @param write       Whether the guest has write access
     * @param expiryHours Number of hours until the code expires
     * @param workspaceId The ID of the workspace (optional)
     * @return The generated guest code
     */
    public String generateShareCode(String resource, String resourceId, String userId, boolean write, int expiryHours, String workspaceId) {
        // Generate a random code
        String shareCode = UUID.randomUUID().toString();

        // Calculate expiry time
        Instant expiresAt = Instant.now().plus(expiryHours, ChronoUnit.HOURS);

        // Create share code info
        ShareCodeInfo codeInfo = new ShareCodeInfo(resource, resourceId, write, userId, expiresAt, workspaceId);

        // Store in Redis with expiration
        String redisKey = REDIS_KEY_PREFIX + shareCode;
        redisTemplate.opsForValue().set(redisKey, codeInfo, expiryHours, TimeUnit.HOURS);

        return shareCode;
    }

    /**
     * Validates a guest code for a specific resource.
     *
     * @param shareCode  The share code to validate
     * @return The guest code information if valid
     * @throws OAuth2AuthenticationException if the code is invalid or expired
     */
    public ShareCodeInfo validateShareCode(String shareCode) {
        if (!StringUtils.hasText(shareCode)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_grant", "Guest code or resource ID is missing", null));
        }

        // Get code info from Redis
        String redisKey = REDIS_KEY_PREFIX + shareCode;
        ShareCodeInfo codeInfo = (ShareCodeInfo) redisTemplate.opsForValue().get(redisKey);

        // Validate code exists
        if (codeInfo == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_grant", "Invalid guest code", null));
        }

        // Validate expiration
        if (Instant.now().isAfter(codeInfo.expiresAt())) {
            // Delete expired code
            redisTemplate.delete(redisKey);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_grant", "Guest code has expired", null));
        }

        return codeInfo;
    }

	public void revokeShareCode(String shareCode) {
		// Delete the share code from Redis
		String redisKey = REDIS_KEY_PREFIX + shareCode;
		redisTemplate.delete(redisKey);
	}

    /**
     * Guest code information record.
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    public record ShareCodeInfo(
            String resource,
            String resourceId,
            boolean write,
            String userId,
            Instant expiresAt,
            String workspaceId) {
        /**
         * Backward compatibility constructor
         */
        public ShareCodeInfo(
                String resource,
                String resourceId,
                boolean write,
                String userId,
                Instant expiresAt) {
            this(resource, resourceId, write, userId, expiresAt, null);
        }
    }
}
