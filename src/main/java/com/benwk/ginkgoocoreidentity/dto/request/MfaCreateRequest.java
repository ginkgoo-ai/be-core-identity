package com.benwk.ginkgoocoreidentity.dto.request;

import com.benwk.ginkgoocoreidentity.domain.enums.MfaType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "MfaCreateRequest", description = "Request object for creating a new MFA method")
public class MfaCreateRequest {
    @Schema(
        title = "MFA Type",
        description = "Type of MFA authentication method",
        example = "TOTP",
        requiredMode = Schema.RequiredMode.REQUIRED,
        allowableValues = {"TOTP", "EMAIL", "SMS"}
    )
    @NotNull(message = "MFA type is required")
    private MfaType type;
    
    @Schema(
        title = "MFA Name",
        description = "User-defined name for the MFA method",
        example = "Work Phone",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        maxLength = 100
    )
    private String name;
}