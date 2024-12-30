package com.benwk.ginkgoocoreidentity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(name = "PasswordResetConfirmRequest", description = "Request object for confirming password reset")
public class PasswordResetConfirmRequest {
    @Schema(
            title = "Reset Token",
            description = "Password reset token received via email",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Reset token is required")
    private String resetToken;
    
    @Schema(
            title = "New Password",
            description = "New password - must be 8-64 characters with at least one digit, lowercase letter, uppercase letter, and special character",
            example = "NewPassword123!",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 8,
            maxLength = 64
    )
    @NotBlank(message = "New password is required")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,64}$",
            message = "Password must be at least 8 characters long and contain at least one digit, " +
                    "one lowercase letter, one uppercase letter, and one special character")
    private String newPassword;
}