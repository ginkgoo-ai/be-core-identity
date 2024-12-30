package com.benwk.ginkgoocoreidentity.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base32;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;

/**
 * Utility class for generating and validating Time-based One-time Passwords (TOTP)
 * Based on RFC 6238 (https://tools.ietf.org/html/rfc6238)
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TotpUtils {
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final Base32 BASE_32 = new Base32();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Default time step (30 seconds)
    private static final long TIME_STEP = 30L;
    
    // Default OTP length (6 digits)
    private static final int PASSWORD_LENGTH = 6;
    
    // Time window for validation (±1 step)
    private static final int VALIDATION_WINDOW = 1;

    /**
     * Verify a TOTP code
     *
     * @param secret Base32 encoded secret key
     * @param code Code to verify
     * @return true if code is valid
     */
    public static boolean verifyCode(String secret, String code) {
        if (code == null || code.length() != PASSWORD_LENGTH) {
            return false;
        }

        try {
            long codeNum = Long.parseLong(code);
            long currentTime = Instant.now().getEpochSecond();
            byte[] secretBytes = BASE_32.decode(secret);

            // Check codes within validation window
            for (int i = -VALIDATION_WINDOW; i <= VALIDATION_WINDOW; i++) {
                long timestamp = currentTime + (i * TIME_STEP);
                if (generateCode(secretBytes, timestamp) == codeNum) {
                    return true;
                }
            }
        } catch (NumberFormatException e) {
            log.debug("Invalid TOTP code format: {}", code);
            return false;
        }

        return false;
    }

    /**
     * Generate a new random secret key
     *
     * @return Base32 encoded secret key
     */
    public static String generateSecret() {
        byte[] buffer = new byte[20];  // 160 bits
        SECURE_RANDOM.nextBytes(buffer);
        return BASE_32.encodeToString(buffer);
    }

    /**
     * Generate URI for QR code
     *
     * @param issuer Service name
     * @param accountName User identifier (usually email)
     * @param secret Secret key
     * @return otpauth URI
     */
    public static String generateQrCodeUri(String issuer, String accountName, String secret) {
        String normalizedIssuer = issuer.replace(':', ' ');
        String normalizedAccount = accountName.replace(':', ' ');
        
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                normalizedIssuer,
                normalizedAccount,
                secret,
                normalizedIssuer,
                PASSWORD_LENGTH,
                TIME_STEP);
    }

    /**
     * Generate TOTP code for a specific time
     *
     * @param secretBytes Decoded secret key
     * @param timestamp Unix timestamp
     * @return Generated code
     */
    private static long generateCode(byte[] secretBytes, long timestamp) {
        try {
            // Convert timestamp to time steps
            long timeSteps = timestamp / TIME_STEP;
            
            // Get byte array of time steps
            byte[] timeBytes = ByteBuffer.allocate(8).putLong(timeSteps).array();

            // Generate HMAC-SHA1
            SecretKeySpec signKey = new SecretKeySpec(secretBytes, HMAC_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(signKey);
            byte[] hash = mac.doFinal(timeBytes);

            // Get offset
            int offset = hash[hash.length - 1] & 0xf;

            // Generate 4-byte code
            long truncatedHash = 0;
            for (int i = 0; i < 4; ++i) {
                truncatedHash <<= 8;
                truncatedHash |= (hash[offset + i] & 0xff);
            }

            // Clear most significant bit
            truncatedHash &= 0x7FFFFFFF;
            
            // Generate n-digit code
            return truncatedHash % (long) Math.pow(10, PASSWORD_LENGTH);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error generating TOTP code", e);
            throw new RuntimeException("Failed to generate TOTP code", e);
        }
    }
}