package com.ginkgooai.core.identity.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@Service
public class GuestCodeService {

    private static final String REDIS_KEY_PREFIX = "guest_code:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Generates a guest code for accessing a specific resource.
     *
     * @param resource    The Type of the resource to be accessed
     * @param resourceId  The ID of the resource to be accessed
     * @param write       Whether the guest has write access
     * @param guestEmail  The email of the guest who will access the resource
     * @param expiryHours Number of hours until the code expires
     * @param workspaceId The ID of the workspace (optional)
     * @return The generated guest code
     */
    public String generateGuestCode(String resource, String resourceId, boolean write, String guestName,
            String guestEmail, String redirectUrl, int expiryHours, String workspaceId) {
        // Generate a random code
        String guestCode = UUID.randomUUID().toString();

        // Calculate expiry time
        Instant expiresAt = Instant.now().plus(expiryHours, ChronoUnit.HOURS);

        // Create guest code info
        GuestCodeInfo codeInfo = new GuestCodeInfo(resource, resourceId, write, guestName, guestEmail, redirectUrl,
                expiresAt, workspaceId);

        // Store in Redis with expiration
        String redisKey = REDIS_KEY_PREFIX + guestCode;
        redisTemplate.opsForValue().set(redisKey, codeInfo, expiryHours, TimeUnit.HOURS);

        return guestCode;
    }

    /**
     * Backward compatibility method for existing code
     */
    public String generateGuestCode(String resource, String resourceId, boolean write, String guestName,
            String guestEmail, String redirectUrl, int expiryHours) {
        return generateGuestCode(resource, resourceId, write, guestName, guestEmail, redirectUrl, expiryHours, null);
    }

    /**
     * Validates a guest code for a specific resource.
     * 
     * @param guestCode  The guest code to validate
     * @param resourceId The resource ID to validate against
     * @return The guest code information if valid
     * @throws OAuth2AuthenticationException if the code is invalid or expired
     */
    public GuestCodeInfo validateGuestCode(String guestCode, String resourceId) {
        if (!StringUtils.hasText(guestCode) || !StringUtils.hasText(resourceId)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_grant", "Guest code or resource ID is missing", null));
        }

        // Get code info from Redis
        String redisKey = REDIS_KEY_PREFIX + guestCode;
        GuestCodeInfo codeInfo = (GuestCodeInfo) redisTemplate.opsForValue().get(redisKey);

        // Validate code exists
        if (codeInfo == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_grant", "Invalid guest code", null));
        }

        // Validate resource ID
        if (!resourceId.equals(codeInfo.resourceId())) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_grant", "Resource ID mismatch", null));
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

    /**
     * Guest code information record.
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    public record GuestCodeInfo(
            String resource,
            String resourceId,
            boolean write,
            String guestName,
            String guestEmail,
            String redirectUrl,
            Instant expiresAt,
            String workspaceId) {
        /**
         * Backward compatibility constructor
         */
        public GuestCodeInfo(
                String resource,
                String resourceId,
                boolean write,
                String guestName,
                String guestEmail,
                String redirectUrl,
                Instant expiresAt) {
            this(resource, resourceId, write, guestName, guestEmail, redirectUrl, expiresAt, null);
        }
    }
}
