package com.benwk.ginkgoocoreidentity.service.verification;

import com.benwk.ginkgoocoreidentity.enums.VerificationStrategy;

public interface EmailVerificationStrategy {
    /**
     * Generate verification credentials (code or token)
     *
     * @param userId User's unique identifier
     * @return Generated verification credentials
     */
    String generateCredential(String clientId, String userId);

    /**
     * Verify the provided credentials
     *
     * @param userId User's unique identifier
     * @param credential Verification code or token
     * @return true if verification successful
     */
    boolean verifyCredential(String userId, String credential);

    /**
     * Get verification type
     *
     * @return The type of verification strategy
     */
    VerificationStrategy getType();
}