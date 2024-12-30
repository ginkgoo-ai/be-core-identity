package com.ginkgooai.core.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(name = "LoginRequest", description = "Request object for MFA Login")
public class LoginRequest {

    @Schema(
            title = "Email",
            description = "User's email address",
            example = "user@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;
   
    private String mfaCode;
}
