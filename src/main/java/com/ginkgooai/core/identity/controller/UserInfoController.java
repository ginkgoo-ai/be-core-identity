package com.ginkgooai.core.identity.controller;

import com.ginkgooai.core.identity.dto.request.*;
import com.ginkgooai.core.identity.dto.response.UserResponse;
import com.ginkgooai.core.identity.exception.InvalidVerificationCodeException;
import com.ginkgooai.core.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for user self-registration and account management")
public class UserInfoController {

    private static final boolean AUTO_LOGIN = false;

    static final String SAVED_REQUEST = "SPRING_SECURITY_SAVED_REQUEST";

    private final UserService userService;

    private final OAuth2AuthorizationRequestResolver defaultAuthorizationRequestResolver;
    
    private final AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register new user", description = "Register a new user with email verification required")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User successfully registered"),
            @ApiResponse(responseCode = "400", description = "Invalid registration data",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Email already registered",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<UserResponse> registerUser(
            @RequestBody @Valid @Parameter(description = "User registration details", required = true)
            RegistrationRequest request,
            HttpServletRequest httpRequest) {
        log.debug("Processing user registration request for email: {}", request.getEmail());

        UserResponse response = userService.createUser(request, httpRequest);

        log.info("Successfully registered user with email: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PatchMapping(value = "/{userId}/email/verification",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Verify user email",
            description = "Verify user's email address using the verification code sent during registration")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email successfully verified"),
            @ApiResponse(responseCode = "400", description = "Invalid verification code",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "410", description = "Verification code expired",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<Void> verifyEmail(
            @Parameter(description = "User ID", required = true)
            @PathVariable @NotBlank String userId,
            @RequestBody @Valid @Parameter(description = "Email verification details", required = true)
            EmailVerificationRequest request) throws InvalidVerificationCodeException {
        log.debug("Processing email verification for user ID: {}", userId);

        userService.verifyEmail(userId, request.getVerificationCode());

        log.info("Successfully verified email for user ID: {}", userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password-resets")
    @Operation(summary = "Initiate password reset",
            description = "Request a password reset token to be sent to the user's email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset initiated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<Void> initiatePasswordReset(
            @RequestBody @Valid @Parameter(description = "Password reset request details", required = true)
            PasswordResetRequest request) {
        log.debug("Processing password reset request for email: {}", request.getEmail());

        userService.initiatePasswordReset(request.getEmail());

        log.info("Password reset initiated for email: {}", request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/password-resets/{token}")
    @Operation(summary = "Complete password reset",
            description = "Reset password using the token received via email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successful"),
            @ApiResponse(responseCode = "400", description = "Invalid password format",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Reset token not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "410", description = "Reset token expired",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<Void> completePasswordReset(
            @PathVariable("token") @Parameter(description = "Password reset token", required = true) String token,
            @RequestBody @Valid @Parameter(description = "Password reset confirmation details", required = true)
            PasswordResetConfirmRequest request) throws InvalidVerificationCodeException {
        log.debug("Processing password reset confirmation");

        userService.confirmPasswordReset(request.getResetToken(), request.getNewPassword());

        log.info("Password reset confirmed successfully");
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{userId}/password")
    @Operation(summary = "Change password",
            description = "Change password for authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid current password",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<Void> changePassword(
            @PathVariable @NotBlank String userId,
            @RequestBody @Valid @Parameter(description = "Password change details", required = true)
            PasswordChangeRequest request) {
        log.debug("Processing password change request for user ID: {}", userId);

        userService.updatePassword(userId, request.getCurrentPassword(), request.getNewPassword());

        log.info("Password changed successfully for user ID: {}", userId);
        return ResponseEntity.ok().build();
    }
}