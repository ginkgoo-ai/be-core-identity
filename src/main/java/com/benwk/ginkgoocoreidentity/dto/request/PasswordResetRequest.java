package com.benwk.ginkgoocoreidentity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(name = "PasswordResetRequest", description = "Request object for initiating password reset")
public class PasswordResetRequest {
    @Schema(
            title = "Email",
            description = "User's registered email address",
            example = "user@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
}
