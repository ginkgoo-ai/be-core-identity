package com.ginkgooai.core.identity.service.verification;

import com.ginkgooai.core.identity.enums.VerificationStrategy;
import com.ginkgooai.core.identity.service.VerificationCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CodeVerificationStrategy implements EmailVerificationStrategy {
    private final VerificationCodeService verificationCodeService;

    @Override
    public String generateCredential(String clientId, String userId) {
        return verificationCodeService.generateCode(userId);
    }

    @Override
    public boolean verifyCredential(String userId, String credential) {
        return verificationCodeService.verifyCode(userId, credential);
    }

    @Override
    public VerificationStrategy getType() {
        return VerificationStrategy.CODE;
    }
}