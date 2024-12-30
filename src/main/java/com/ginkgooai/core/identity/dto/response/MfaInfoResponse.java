package com.ginkgooai.core.identity.dto.response;

import com.ginkgooai.core.identity.domain.MfaInfo;
import com.ginkgooai.core.identity.domain.enums.MfaStatus;
import com.ginkgooai.core.identity.domain.enums.MfaType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(name = "MfaInfoResponse", description = "Response object containing MFA method details")
public class MfaInfoResponse {
    @Schema(
        title = "MFA ID",
        description = "Unique identifier for the MFA method",
        example = "550e8400-e29b-41d4-a716-446655440000"
    )
    private String id;

    @Schema(
        title = "MFA Type",
        description = "Type of MFA method",
        example = "TOTP",
        allowableValues = {"TOTP", "EMAIL", "SMS"}
    )
    private MfaType type;

    @Schema(
        title = "MFA Name",
        description = "User-defined name for the MFA method",
        example = "Work Phone"
    )
    private String name;

    @Schema(
        title = "MFA Status",
        description = "Current status of the MFA method",
        example = "ENABLED",
        allowableValues = {"DISABLED", "PENDING", "ENABLED"}
    )
    private MfaStatus status;

    @Schema(
        title = "Is Default",
        description = "Whether this is the default MFA method",
        example = "true"
    )
    private boolean isDefault;

    @Schema(
        title = "Last Verified At",
        description = "Timestamp of last successful verification",
        example = "2024-01-07T10:30:00Z",
        format = "date-time"
    )
    private LocalDateTime lastVerifiedAt;

    @Schema(
        title = "Secret Key",
        description = "TOTP secret key (only included during TOTP setup)",
        example = "JBSWY3DPEHPK3PXP",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String secretKey;
    
    public static MfaInfoResponse from(MfaInfo mfaInfo) {
        MfaInfoResponse response = new MfaInfoResponse();
        response.setId(mfaInfo.getId());
        response.setType(mfaInfo.getType());
        response.setStatus(mfaInfo.getStatus());
        response.setDefault(mfaInfo.isDefault());
        response.setLastVerifiedAt(mfaInfo.getLastVerifiedAt());
        
        // Only include secret key for TOTP during setup
        if (mfaInfo.getType() == MfaType.TOTP && mfaInfo.getStatus() == MfaStatus.PENDING) {
            response.setSecretKey(mfaInfo.getSecretKey());
        }
        
        return response;
    }
}