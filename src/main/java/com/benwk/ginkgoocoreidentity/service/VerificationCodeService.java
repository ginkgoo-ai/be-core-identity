package com.benwk.ginkgoocoreidentity.service;

import com.benwk.ginkgoocoreidentity.domain.TokenIdentity;
import com.benwk.ginkgoocoreidentity.exception.InvalidVerificationCodeException;
import com.benwk.ginkgoocoreidentity.exception.TokenRequestTooFrequentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class VerificationCodeService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String VERIFICATION_CODE_PREFIX = "verification:code:";
    private static final String PASSWORD_RESET_PREFIX = "verification:password-reset:";
    private static final String COOLDOWN_PREFIX = "verification:cooldown:";
    private static final String EMAIL_VERIFICATION_TOKEN_PREFIX = "verification:email-token:";
    private static final long CODE_EXPIRATION_SECONDS = 300; // 5 minutes for email verification
    private static final long PASSWORD_RESET_EXPIRATION_SECONDS = 900; // 15 minutes for password reset
    private static final long COOLDOWN_SECONDS = 60;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Generate a URL token for email verification
     * Encodes the userId within the token for efficient verification
     *
     * @param userId User's unique identifier
     * @return Base64URL encoded verification token
     * @throws TokenRequestTooFrequentException if request is within cooldown period
     */
    public String generateEmailVerificationToken(String clientId, String userId) {
        String cooldownKey = COOLDOWN_PREFIX + clientId + ":" + userId;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new TokenRequestTooFrequentException("Please wait before requesting a new verification link");
        }

        // Generate token with embedded userId for efficient verification
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String tokenData = clientId + ":" + userId + ":" + Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenData.getBytes(StandardCharsets.UTF_8));

        // Store token hash with expiration
        String hashedToken = hashCode(token);
        String tokenKey = EMAIL_VERIFICATION_TOKEN_PREFIX + clientId + ":" + userId;

        redisTemplate.opsForValue().set(tokenKey, hashedToken, CODE_EXPIRATION_SECONDS, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(cooldownKey, "1", COOLDOWN_SECONDS, TimeUnit.SECONDS);

        return token;
    }

    /**
     * Verify email verification token
     * Extracts userId from token for O(1) lookup
     *
     * @param token The verification token
     * @return User ID associated with the token
     * @throws InvalidVerificationCodeException if token is invalid or expired
     */
    public TokenIdentity verifyEmailToken(String token) throws InvalidVerificationCodeException {
        try {
            // Decode token and extract userId
            TokenIdentity tokenFix = new TokenIdentity(token); 
            String tokenKey = EMAIL_VERIFICATION_TOKEN_PREFIX + tokenFix;

            // Validate token hash
            String storedHash = redisTemplate.opsForValue().get(tokenKey);
            if (storedHash == null || !storedHash.equals(hashCode(token))) {
                throw new InvalidVerificationCodeException("Invalid or expired token");
            }

            // Invalidate token after successful verification
            invalidateEmailVerificationToken(tokenFix);
            return tokenFix;
        } catch (IllegalArgumentException e) {
            log.warn("Failed to decode verification token", e);
            throw new InvalidVerificationCodeException("Invalid token format");
        }
    }

    /**
     * Invalidates an email verification token and its cooldown
     *
     * @param tokenIdentity User's token unique identifier
     */
    public void invalidateEmailVerificationToken(TokenIdentity tokenIdentity) {
        String tokenKey = EMAIL_VERIFICATION_TOKEN_PREFIX + tokenIdentity;
        String cooldownKey = COOLDOWN_PREFIX + tokenIdentity;
        redisTemplate.delete(tokenKey);
        redisTemplate.delete(cooldownKey);
    }

    /**
     * Generates a verification code for email verification
     *
     * @param userId User's unique identifier
     * @return 6-digit verification code
     * @throws TokenRequestTooFrequentException if request is within cooldown period
     */
    public String generateCode(String userId) {
        String cooldownKey = COOLDOWN_PREFIX + userId;

        // Check cooldown period
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new TokenRequestTooFrequentException("Please wait before requesting a new code");
        }

        // Generate 6-digit code
        String code = String.format("%06d", new SecureRandom().nextInt(1000000));
        String codeKey = VERIFICATION_CODE_PREFIX + userId;

        // Store code with expiration
        redisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRATION_SECONDS, TimeUnit.SECONDS);
        // Set cooldown
        redisTemplate.opsForValue().set(cooldownKey, "1", COOLDOWN_SECONDS, TimeUnit.SECONDS);

        log.debug("Generated verification code for user: {}", userId);
        return code;
    }


    /**
     * Verifies a verification code or reset token
     *
     * @param userId User's unique identifier
     * @param code   Code or token to verify
     * @return true if code/token is valid, false otherwise
     */
    public boolean verifyCode(String userId, String code) {
        // Try verification code first
        String codeKey = VERIFICATION_CODE_PREFIX + userId;
        String storedCode = redisTemplate.opsForValue().get(codeKey);

        if (storedCode != null) {
            boolean isValid = storedCode.equals(code);
            if (isValid) {
                invalidateCode(userId);
            }
            return isValid;
        }

        // Try password reset token
        String tokenKey = PASSWORD_RESET_PREFIX + userId;
        String storedHash = redisTemplate.opsForValue().get(tokenKey);

        if (storedHash != null) {
            boolean isValid = storedHash.equals(hashCode(code));
            if (isValid) {
                invalidatePasswordResetToken(userId);
            }
            return isValid;
        }

        return false;
    }

    /**
     * Generates a password reset token containing encoded userId for efficient lookup
     *
     * @param userId User's unique identifier
     * @return Base64URL encoded reset token
     * @throws TokenRequestTooFrequentException if request is within cooldown period
     */
    public String generatePasswordResetToken(String userId) {
        String cooldownKey = COOLDOWN_PREFIX + "pwd:" + userId;

        // Check cooldown period
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new TokenRequestTooFrequentException("Please wait before requesting another password reset");
        }

        // Generate secure random bytes
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);

        // Encode userId within the token for efficient retrieval
        String tokenData = userId + ":" + Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenData.getBytes(StandardCharsets.UTF_8));

        // Store hashed token with expiration
        String hashedToken = hashCode(token);
        String tokenKey = PASSWORD_RESET_PREFIX + userId;

        redisTemplate.opsForValue().set(tokenKey, hashedToken, PASSWORD_RESET_EXPIRATION_SECONDS, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(cooldownKey, "1", COOLDOWN_SECONDS, TimeUnit.SECONDS);

        return token;
    }

    /**
     * Extracts and validates userId from the password reset token
     * Performs O(1) lookup using encoded userId instead of scanning all keys
     *
     * @param token The password reset token
     * @return Associated user ID
     * @throws InvalidVerificationCodeException if token is invalid or expired
     */
    public String getUserIdFromToken(String token) throws InvalidVerificationCodeException {
        try {
            // Decode token and extract userId
            String tokenData = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = tokenData.split(":", 2);
            if (parts.length != 2) {
                throw new InvalidVerificationCodeException("Invalid token format");
            }

            String userId = parts[0];
            String tokenKey = PASSWORD_RESET_PREFIX + userId;

            // Validate token hash
            String storedHash = redisTemplate.opsForValue().get(tokenKey);
            if (storedHash == null || !storedHash.equals(hashCode(token))) {
                throw new InvalidVerificationCodeException("Invalid or expired token");
            }

            return userId;
        } catch (IllegalArgumentException e) {
            log.warn("Failed to decode reset token", e);
            throw new InvalidVerificationCodeException("Invalid token format");
        }
    }

    /**
     * Invalidates an email verification code
     *
     * @param userId User's unique identifier
     */
    public void invalidateCode(String userId) {
        String codeKey = VERIFICATION_CODE_PREFIX + userId;
        String cooldownKey = COOLDOWN_PREFIX + userId;
        redisTemplate.delete(codeKey);
        redisTemplate.delete(cooldownKey);
    }

    /**
     * Invalidates a password reset token
     *
     * @param userId User's unique identifier
     */
    public void invalidatePasswordResetToken(String userId) {
        String tokenKey = PASSWORD_RESET_PREFIX + userId;
        String cooldownKey = COOLDOWN_PREFIX + "pwd:" + userId;
        redisTemplate.delete(tokenKey);
        redisTemplate.delete(cooldownKey);
    }

    /**
     * Creates a SHA-256 hash of the provided token
     *
     * @param code Token to be hashed
     * @return Base64 encoded hash
     */
    private String hashCode(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }
}