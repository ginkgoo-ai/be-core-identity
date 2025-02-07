package com.ginkgooai.core.identity.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Type of MFA authentication method")
public enum MfaType {
    @Schema(description = "No MFA authentication")
    NONE,
    
    @Schema(description = "Time-based One-time Password")
    TOTP,
    
    @Schema(description = "Email verification code")
    EMAIL,
    
    @Schema(description = "SMS verification code")
    SMS
}
