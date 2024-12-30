package com.benwk.ginkgoocoreidentity.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status of MFA method")
public enum MfaStatus {
    @Schema(description = "MFA is disabled")
    DISABLED,
    
    @Schema(description = "MFA is being set up but not verified")
    PENDING,
    
    @Schema(description = "MFA is enabled and verified")
    ENABLED
}