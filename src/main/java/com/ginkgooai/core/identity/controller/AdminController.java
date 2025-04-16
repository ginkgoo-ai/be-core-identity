package com.ginkgooai.core.identity.controller;

import com.ginkgooai.core.identity.dto.request.UserActivationRequest;
import com.ginkgooai.core.identity.dto.response.UserResponse;
import com.ginkgooai.core.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Management", description = "APIs for admin management with api-key")
public class AdminController {

	private final UserService userService;

	@PatchMapping("/users/{userId}/activation")
	@Operation(summary = "Activate or deactivate user",
			description = "Activate or deactivate a user by ID. Requires ADMIN role.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "User activation status updated successfully"),
			@ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "404", description = "User not found",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)) })
	public ResponseEntity<UserResponse> toggleUserActivation(@PathVariable @NotBlank String userId,
			@RequestBody @Valid @Parameter(description = "Activation details",
					required = true) UserActivationRequest request) {
		log.debug("Processing user activation toggle for user ID: {}", userId);

		UserResponse userResponse = userService.toggleUserActivation(userId, request.getActive());

		log.info("Successfully toggled activation for user ID: {}, new status: {}", userId,
				request.getActive() ? "ACTIVE" : "INACTIVE");
		return ResponseEntity.ok(userResponse);
	}

}