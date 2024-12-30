package com.benwk.ginkgoocoreidentity.service.verification;

import com.benwk.ginkgoocoreidentity.domain.TokenIdentity;
import com.benwk.ginkgoocoreidentity.enums.VerificationStrategy;
import com.benwk.ginkgoocoreidentity.exception.InvalidVerificationCodeException;
import com.benwk.ginkgoocoreidentity.service.VerificationCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UrlTokenVerificationStrategy implements EmailVerificationStrategy {
    private final VerificationCodeService verificationCodeService;

    @Override
    public String generateCredential(String clientId, String userId) {
        return verificationCodeService.generateEmailVerificationToken(clientId, userId);
    }

    @Override
    public boolean verifyCredential(String userId, String credential) {
        try {
            TokenIdentity tokenIdentity = verificationCodeService.verifyEmailToken(credential);
            return userId.equals(tokenIdentity.getUserId());
        } catch (InvalidVerificationCodeException e) {
            return false;
        }
    }

    @Override
    public VerificationStrategy getType() {
        return VerificationStrategy.URL_TOKEN;
    }
}