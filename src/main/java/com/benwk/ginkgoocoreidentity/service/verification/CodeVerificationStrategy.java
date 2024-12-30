package com.benwk.ginkgoocoreidentity.service.verification;

import com.benwk.ginkgoocoreidentity.enums.VerificationStrategy;
import com.benwk.ginkgoocoreidentity.service.VerificationCodeService;
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