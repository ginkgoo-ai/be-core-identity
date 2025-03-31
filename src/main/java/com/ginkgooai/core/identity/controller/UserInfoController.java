package com.ginkgooai.core.identity.controller;

import com.ginkgooai.core.identity.domain.UserInfo;
import com.ginkgooai.core.identity.dto.request.*;
import com.ginkgooai.core.identity.dto.response.UserResponse;
import com.ginkgooai.core.identity.exception.InvalidVerificationCodeException;
import com.ginkgooai.core.identity.service.UserService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for user self-registration and account management")
public class UserInfoController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get user info", description = "MVP:Retrieve information about the currently authenticated user")
    public ResponseEntity<UserResponse> getUserInfo(@AuthenticationPrincipal Jwt jwt) {
        log.debug("Retrieving info for user: {}", jwt.getSubject());
        UserInfo userInfo = userService.getUserById(jwt.getSubject());
        return ResponseEntity.ok(UserResponse.from(userInfo));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user info", description = "Retrieve information about the currently authenticated user")
    @Hidden
    public ResponseEntity<UserResponse> getUserInfo(@PathVariable String userId) {
        log.debug("Retrieving info for user: {}", userId);
        UserInfo userInfo = userService.getUserById(userId);
        return ResponseEntity.ok(UserResponse.from(userInfo));
    }

    @GetMapping("")
    @Operation(summary = "Get users info by ids",
            description = "Retrieve information about multiple users by their IDs")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user information"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<List<UserResponse>> getUsersByIds(
            @RequestParam("ids") @Size(min = 1, max = 100) List<String> userIds) {
        log.debug("Retrieving info for users: {}", userIds);
        List<UserResponse> users = userService.getUsersByIds(userIds);
        return ResponseEntity.ok(users);
    }

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

        UserResponse response;
        if (request.isOnlineRegistration()) {
            response = userService.createUser(request, httpRequest);
        } else {
            response = userService.createTempUser(request);
        }

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
    @Hidden
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
    @Hidden
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
    @Hidden
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
    @Hidden
    public ResponseEntity<Void> changePassword(
            @PathVariable @NotBlank String userId,
            @RequestBody @Valid @Parameter(description = "Password change details", required = true)
            PasswordChangeRequest request) {
        log.debug("Processing password change request for user ID: {}", userId);

        userService.updatePassword(userId, request.getCurrentPassword(), request.getNewPassword());

        log.info("Password changed successfully for user ID: {}", userId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{userId}")
    @Operation(summary = "Patch User info",
            description = "Patch user info for authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Patch User info successfully"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<Void> patchUserInfo(
            @PathVariable @NotBlank String userId,
            @RequestBody @Valid @Parameter(description = "Patch user info request details", required = true)
            PatchUserRequest request) {
        log.debug("Patch user info for user ID: {}", userId);

        userService.patchUserInfo(userId, request.getPictureUrl(), request.getName());

        log.info("Patch user info for user ID: {}", userId);
        return ResponseEntity.ok().build();
    }


    /**
     * GET /users : Search users with dynamic filters
     *
     * @param email Exact match for email
     * @param name  Partial match for name
     * @return UserResponse with matched user
     */
    @GetMapping("/user")
    @Operation(summary = "Search users", description = "Search users with multiple filter criteria")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @Hidden
    public ResponseEntity<UserResponse> searchUsers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String name) {

        UserInfo userInfo = userService.getUserBySpecification(email, name);
        return ResponseEntity.ok(UserResponse.from(userInfo));
    }

}