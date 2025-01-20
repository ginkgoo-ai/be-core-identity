package com.ginkgooai.core.identity.service;

import com.ginkgooai.core.identity.domain.MfaInfo;
import com.ginkgooai.core.identity.domain.UserInfo;
import com.ginkgooai.core.identity.domain.enums.MfaStatus;
import com.ginkgooai.core.identity.domain.enums.MfaType;
import com.ginkgooai.core.identity.dto.request.BackupCodesResponse;
import com.ginkgooai.core.identity.dto.request.MfaCreateRequest;
import com.ginkgooai.core.identity.dto.response.MfaInfoResponse;
import com.ginkgooai.core.identity.dto.response.MfaSendResponse;
import com.ginkgooai.core.identity.exception.ConflictException;
import com.ginkgooai.core.identity.exception.InvalidVerificationCodeException;
import com.ginkgooai.core.identity.exception.MfaLockedException;
import com.ginkgooai.core.identity.exception.ResourceNotFoundException;
import com.ginkgooai.core.identity.repository.MfaInfoRepository;
import com.ginkgooai.core.identity.repository.UserRepository;
import com.ginkgooai.core.identity.util.TotpUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.apache.commons.codec.binary.Base32;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class MfaService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base32 base32 = new Base32();
    private static final int BACKUP_CODES_COUNT = 10;
    private static final int MAX_VERIFICATION_ATTEMPTS = 5;
    private final MfaInfoRepository mfaInfoRepository;
    private final UserRepository userRepository;
    private final VerificationCodeService verificationCodeService;
    private final EmailService emailService;

    public List<MfaInfoResponse> listMfaMethods(String userId) {
        log.debug("Listing MFA methods for user: {}", userId);
        return mfaInfoRepository.findByUserId(userId).stream()
                .map(MfaInfoResponse::from)
                .collect(Collectors.toList());
    }

    public MfaInfoResponse createMfa(String userId, MfaCreateRequest request) {
        log.debug("Creating new MFA for user: {}, type: {}", userId, request.getType());

        UserInfo user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Check if MFA of this type already exists
        if (mfaInfoRepository.findByUserIdAndType(userId, request.getType()).isPresent()) {
            throw new ConflictException("MFA", "type", request.getType().toString());
        }

        MfaInfo mfaInfo = new MfaInfo();
        mfaInfo.setUser(user);
        mfaInfo.setType(request.getType());
        mfaInfo.setStatus(MfaStatus.PENDING);

        // Generate secret key for TOTP
        if (request.getType() == MfaType.TOTP) {
            String secretKey = generateTotpSecret();
            mfaInfo.setSecretKey(secretKey);
        }

        // Set as default if it's the first MFA method
        if (mfaInfoRepository.countByUserId(userId) == 0) {
            mfaInfo.setDefault(true);
        }

        mfaInfo = mfaInfoRepository.save(mfaInfo);
        return MfaInfoResponse.from(mfaInfo);
    }

    public MfaInfoResponse getMfa(String userId, String mfaId) {
        log.debug("Getting MFA details - userId: {}, mfaId: {}", userId, mfaId);
        return MfaInfoResponse.from(getMfaInfo(userId, mfaId));
    }

    public void deleteMfa(String userId, String mfaId) {
        log.debug("Deleting MFA - userId: {}, mfaId: {}", userId, mfaId);
        MfaInfo mfaInfo = getMfaInfo(userId, mfaId);

        // Prevent deletion of the only active MFA method
        if (mfaInfo.isDefault() && mfaInfoRepository.countByUserIdAndStatus(userId, MfaStatus.ENABLED) == 1) {
            throw new IllegalStateException("Cannot delete the only active MFA method");
        }

        mfaInfoRepository.delete(mfaInfo);
    }

    /**
     * Send or prepare MFA verification code
     *
     * @param userId  User identifier
     * @param mfaInfo MFA configuration
     * @return MFA related information (e.g., TOTP QR code URL)
     */
    public MfaSendResponse sendMfaCode(String userId, MfaInfo mfaInfo) {
        log.debug("Sending MFA code for user: {}, mfaId: {}", userId, mfaInfo.getId());

        UserInfo user = mfaInfo.getUser();
        if (mfaInfo.getStatus() != MfaStatus.ENABLED) {
            throw new IllegalStateException("MFA is not enabled");
        }

        try {
            switch (mfaInfo.getType()) {
                case EMAIL -> {
                    // Generate and send email verification code
                    String code = verificationCodeService.generateCode(userId);
                    emailService.sendMfaCodeEmail(user.getEmail(), code);
                    return new MfaSendResponse(MfaType.EMAIL, null);
                }
                case SMS -> {
                    // Generate and send SMS verification code
                    String code = verificationCodeService.generateCode(userId);
                    // TODO: Implement SMS service integration
                    // smsService.sendVerificationCode(user.getPhone(), code);
                    return new MfaSendResponse(MfaType.SMS, null);
                }
                case TOTP -> {
                    // For TOTP, return necessary information instead of sending a code
                    String qrCodeUrl = TotpUtils.generateQrCodeUri(
                            user.getEmail(),
                            mfaInfo.getSecretKey(),
                            "ginkgoo"
                    );
                    return new MfaSendResponse(MfaType.TOTP, qrCodeUrl);
                }
                default -> throw new UnsupportedOperationException("Unsupported MFA type: " + mfaInfo.getType());
            }
        } catch (Exception e) {
            log.error("Failed to send MFA code for user: {}", userId, e);
            throw new RuntimeException("Failed to send MFA code", e);
        }
    }

    /**
     * Get and send MFA code using default MFA configuration for the specified type
     *
     * @param userId  User identifier
     * @param mfaType Type of MFA verification
     * @return MFA send response containing type and additional information
     */
    public MfaSendResponse sendMfaCode(String userId, MfaType mfaType) {
        log.debug("Sending MFA code for user: {} with type: {}", userId, mfaType);

        MfaInfo mfaInfo = mfaInfoRepository.findByUserIdAndType(userId, mfaType)
                .orElseThrow(() -> new ResourceNotFoundException("MFA", "type", mfaType.toString()));

        return sendMfaCode(userId, mfaInfo);
    }

    /**
     * Send MFA code using the user's default MFA configuration
     *
     * @param userId User identifier
     * @return MFA send response containing type and additional information
     */
    public MfaSendResponse sendDefaultMfaCode(String userId) {
        log.debug("Sending default MFA code for user: {}", userId);

        MfaInfo defaultMfa = mfaInfoRepository.findByUserIdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "MFA", userId));
        return sendMfaCode(userId, defaultMfa);
    }

    public void verifyMfa(String userId, String mfaId, String code) throws InvalidVerificationCodeException {
        log.debug("Verifying MFA setup - userId: {}, mfaId: {}", userId, mfaId);
        MfaInfo mfaInfo;
        mfaInfo = getMfaInfo(userId, mfaId);

        if (null != mfaInfo.getAttemptsCount() && mfaInfo.getAttemptsCount() >= MAX_VERIFICATION_ATTEMPTS) {
            throw new MfaLockedException("Too many failed verification attempts");
        }

        boolean isValid;
        switch (mfaInfo.getType()) {
            case TOTP:
                isValid = verifyTotpCode(mfaInfo.getSecretKey(), code);
                break;
            case EMAIL:
                isValid = verifyEmailCode(userId, code);
                break;
            case SMS:
                isValid = verifySmsCode(userId, code);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported MFA type");
        }

        if (!isValid) {
            mfaInfo.incrementAttempts();
            mfaInfoRepository.save(mfaInfo);
            throw new InvalidVerificationCodeException("Invalid verification code");
        }

        mfaInfo.setStatus(MfaStatus.ENABLED);
        mfaInfo.resetAttempts();
        mfaInfo.recordSuccessfulVerification();
        mfaInfoRepository.save(mfaInfo);
    }

    public void setDefaultMfa(String userId, String mfaId) {
        log.debug("Setting default MFA - userId: {}, mfaId: {}", userId, mfaId);
        MfaInfo mfaInfo = getMfaInfo(userId, mfaId);

        if (mfaInfo.getStatus() != MfaStatus.ENABLED) {
            throw new IllegalStateException("Cannot set unverified MFA as default");
        }

        // Clear current default
        mfaInfoRepository.clearDefaultMfaForUser(userId);

        mfaInfo.setDefault(true);
        mfaInfoRepository.save(mfaInfo);
    }

    public BackupCodesResponse generateBackupCodes(String userId) {
        log.debug("Generating backup codes for user: {}", userId);

        // Check if user has any active MFA
        if (mfaInfoRepository.countByUserIdAndStatus(userId, MfaStatus.ENABLED) == 0) {
            throw new IllegalStateException("No active MFA configured");
        }

        // Generate new backup codes
        List<String> backupCodes = generateNewBackupCodes();

        // Save hashed backup codes for all user's MFA methods
        String hashedCodes = hashBackupCodes(backupCodes);
        mfaInfoRepository.findByUserId(userId).forEach(mfa -> {
            mfa.setBackupCodes(hashedCodes);
            mfaInfoRepository.save(mfa);
        });

        return new BackupCodesResponse(backupCodes);
    }

    private MfaInfo getMfaInfo(String userId, String mfaId) {
        MfaInfo mfaInfo = ObjectUtils.isEmpty(mfaId) ? mfaInfoRepository.findByUserIdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("MFA", "userId", userId))
                : mfaInfoRepository.findById(mfaId)
                .orElseThrow(() -> new ResourceNotFoundException("MFA", "id", mfaId));

        if (!mfaInfo.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("MFA", "id", mfaId);
        }

        return mfaInfo;
    }

    private String generateTotpSecret() {
        byte[] buffer = new byte[20];
        SECURE_RANDOM.nextBytes(buffer);
        return base32.encodeToString(buffer);
    }

    private boolean verifyTotpCode(String secretKey, String code) {
        return TotpUtils.verifyCode(secretKey, code);
    }

    private boolean verifyEmailCode(String userId, String code) {
        // Implementation depends on your email verification service
        return true;
    }

    private boolean verifySmsCode(String userId, String code) {
        // Implementation depends on your SMS verification service
        return true;
    }

    private List<String> generateNewBackupCodes() {
        return IntStream.range(0, BACKUP_CODES_COUNT)
                .mapToObj(i -> String.format("%08d", SECURE_RANDOM.nextInt(100000000)))
                .collect(Collectors.toList());
    }

    private String hashBackupCodes(List<String> codes) {
        return codes.stream()
                .map(this::hashCode)
                .collect(Collectors.joining(","));
    }

    private String hashCode(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash code", e);
        }
    }
}