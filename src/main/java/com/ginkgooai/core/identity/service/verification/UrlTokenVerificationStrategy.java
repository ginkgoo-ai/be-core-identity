package com.ginkgooai.core.identity.service.verification;

import com.ginkgooai.core.identity.domain.TokenIdentity;
import com.ginkgooai.core.identity.enums.VerificationStrategy;
import com.ginkgooai.core.identity.exception.InvalidVerificationCodeException;
import com.ginkgooai.core.identity.service.VerificationCodeService;
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