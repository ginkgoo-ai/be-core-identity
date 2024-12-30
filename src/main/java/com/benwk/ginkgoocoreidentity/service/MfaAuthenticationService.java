package com.benwk.ginkgoocoreidentity.service;

import com.benwk.ginkgoocoreidentity.domain.UserInfo;
import com.benwk.ginkgoocoreidentity.dto.response.MfaSendResponse;
import com.benwk.ginkgoocoreidentity.exception.InvalidVerificationCodeException;
import com.benwk.ginkgoocoreidentity.exception.ResourceNotFoundException;
import com.benwk.ginkgoocoreidentity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MfaAuthenticationService {
    private static final String MFA_SESSION_KEY = "mfa:session:";
    private static final long MFA_SESSION_DURATION = 300; // 5 minutes
    private final MfaService mfaService;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public Map<String, Object> handleMfa(String email, String mfaCode) throws InvalidVerificationCodeException {
        UserInfo user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        
        if (!user.isMfaEnabled()) {
            return new HashMap<>();
        }

        String mfaSessionKey = MFA_SESSION_KEY + user.getId();
        String mfaSession = redisTemplate.opsForValue().get(mfaSessionKey);

        if (mfaSession != null && !ObjectUtils.isEmpty(mfaCode)) {
            mfaService.verifyMfa(user.getId(), null, mfaCode);
            redisTemplate.delete(mfaSessionKey);
            return Map.of("mfaVerified", true);
        }

        // Create a new MFA session and send MFA code
        redisTemplate.opsForValue().set(mfaSessionKey, "1", MFA_SESSION_DURATION, TimeUnit.SECONDS);
        MfaSendResponse mfaSendResponse = mfaService.sendDefaultMfaCode(user.getId());

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("requireMfa", true);
        responseBody.put("mfaType", mfaSendResponse.getType());
        return responseBody;
    }
}