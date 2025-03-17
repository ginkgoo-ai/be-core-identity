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
public class GuestCodeService {

    private static final String REDIS_KEY_PREFIX = "guest_code:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Generates a guest code for accessing a specific resource.
     * 
     * @param resourceId The ID of the resource to be accessed
     * @param ownerEmail The email of the resource owner
     * @param guestEmail The email of the guest who will access the resource
     * @param expiryHours Number of hours until the code expires
     * @return The generated guest code
     */
    public String generateGuestCode(String resourceId, String ownerEmail, String guestEmail, int expiryHours) {
        // Generate a random code
        String guestCode = UUID.randomUUID().toString();
        
        // Calculate expiry time
        Instant expiresAt = Instant.now().plus(expiryHours, ChronoUnit.HOURS);
        
        // Create guest code info
        GuestCodeInfo codeInfo = new GuestCodeInfo(resourceId, ownerEmail, guestEmail, expiresAt);
        
        // Store in Redis with expiration
        String redisKey = REDIS_KEY_PREFIX + guestCode;
        redisTemplate.opsForValue().set(redisKey, codeInfo, expiryHours, TimeUnit.HOURS);

        return guestCode;
    }

    /**
     * Validates a guest code for a specific resource.
     * 
     * @param guestCode The guest code to validate
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
        GuestCodeInfo codeInfo = (GuestCodeInfo)redisTemplate.opsForValue().get(redisKey);
        
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
            String resourceId,
            String ownerEmail,
            String guestEmail,
            Instant expiresAt
    ) {}
}
