package com.ginkgooai.core.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "MfaUpdateRequest", description = "Request object for updating an existing MFA method")
public class MfaUpdateRequest {
    @Schema(
        title = "MFA Name",
        description = "New name for the MFA method",
        example = "Personal Phone",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        maxLength = 100
    )
    private String name;
}
