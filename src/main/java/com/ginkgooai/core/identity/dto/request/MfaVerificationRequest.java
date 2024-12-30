package com.ginkgooai.core.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(name = "MfaVerificationRequest", description = "Request object for verifying MFA setup or authentication")
public class MfaVerificationRequest {
    @Schema(
        title = "Verification Code",
        description = "6-digit verification code",
        example = "123456",
        requiredMode = Schema.RequiredMode.REQUIRED,
        pattern = "^[0-9]{6}$",
        minLength = 6,
        maxLength = 6
    )
    @NotBlank(message = "Verification code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Invalid verification code format")
    private String code;
}
