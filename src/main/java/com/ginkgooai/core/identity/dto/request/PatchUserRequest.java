package com.ginkgooai.core.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author: david
 * @date: 17:17 2025/2/25
 */

@Data
@Schema(name = "PatchUserRequest", description = "Request object for patch user info")
public class PatchUserRequest {

    @Schema(
            title = "File Id",
            description = "User's photo id",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String fileId;

    @Schema(
            title = "First Name",
            description = "User's first name",
            example = "John",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String firstName;

    @Schema(
            title = "Last Name",
            description = "User's last name",
            example = "Doe",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String lastName;

}
