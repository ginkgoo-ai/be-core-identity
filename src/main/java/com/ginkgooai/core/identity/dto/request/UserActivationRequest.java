package com.ginkgooai.core.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User activation/deactivation request")
public class UserActivationRequest {

	@NotNull(message = "active status must not be null")
	@Schema(description = "Whether to activate (true) or deactivate (false) the user", required = true)
	private Boolean active;

}