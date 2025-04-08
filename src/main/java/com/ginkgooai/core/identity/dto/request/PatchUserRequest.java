package com.ginkgooai.core.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author: david
 * @date: 17:17 2025/2/25
 */

@Data
@Schema(name = "PatchUserRequest", description = "Request object for patch user info")
public class PatchUserRequest {

    @Schema(
            title = "Picture url",
            description = "User's picture id",
            example = "https://example.com/picture.jpg",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String pictureUrl;

    @Schema(description = "User's first name")
    @NotBlank(message = "First name is required")
    private String firstName;

    @Schema(description = "User's last name")
    @NotBlank(message = "Last name is required")
    private String lastName;

}
