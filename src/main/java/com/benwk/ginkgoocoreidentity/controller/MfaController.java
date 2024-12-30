package com.benwk.ginkgoocoreidentity.controller;

import com.benwk.ginkgoocoreidentity.dto.request.BackupCodesResponse;
import com.benwk.ginkgoocoreidentity.dto.request.MfaCreateRequest;
import com.benwk.ginkgoocoreidentity.dto.request.MfaVerificationRequest;
import com.benwk.ginkgoocoreidentity.dto.response.MfaInfoResponse;
import com.benwk.ginkgoocoreidentity.exception.InvalidVerificationCodeException;
import com.benwk.ginkgoocoreidentity.service.MfaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users/{userId}/mfa")
@Tag(name = "MFA Management", description = "APIs for managing Multi-Factor Authentication")
@RequiredArgsConstructor
@Validated
@Slf4j
public class MfaController {

    private final MfaService mfaService;

    @GetMapping
    @Operation(summary = "List user's MFA methods", 
            description = "Get all configured MFA methods for a specific user")
    @ApiResponse(responseCode = "200", description = "List of configured MFA methods")
    public ResponseEntity<List<MfaInfoResponse>> getMfaMethods(
            @PathVariable @Parameter(description = "User ID", required = true) String userId) {
        List<MfaInfoResponse> mfaMethods = mfaService.listMfaMethods(userId);
        return ResponseEntity.ok(mfaMethods);
    }

    @PostMapping
    @Operation(summary = "Create new MFA method", 
            description = "Initialize a new MFA method for the user")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "MFA setup initialized successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid MFA type or configuration"),
        @ApiResponse(responseCode = "409", description = "MFA of this type already exists")
    })
    public ResponseEntity<MfaInfoResponse> createMfa(
            @PathVariable @Parameter(description = "User ID", required = true) String userId,
            @RequestBody @Valid @Parameter(description = "MFA setup details", required = true)
            MfaCreateRequest request) {
        log.debug("Creating MFA for user: {}, type: {}", userId, request.getType());
        MfaInfoResponse response = mfaService.createMfa(userId, request);
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.getId())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{mfaId}")
    @Operation(summary = "Get MFA details", 
            description = "Get details of a specific MFA method")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "MFA details retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "MFA not found")
    })
    public ResponseEntity<MfaInfoResponse> getMfa(
            @PathVariable @Parameter(description = "User ID", required = true) String userId,
            @PathVariable @Parameter(description = "MFA ID", required = true) String mfaId) {
        log.debug("Getting MFA details - userId: {}, mfaId: {}", userId, mfaId);
        MfaInfoResponse mfa = mfaService.getMfa(userId, mfaId);
        return ResponseEntity.ok(mfa);
    }

    @DeleteMapping("/{mfaId}")
    @Operation(summary = "Delete MFA method", 
            description = "Remove a specific MFA method")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "MFA deleted successfully"),
        @ApiResponse(responseCode = "404", description = "MFA not found"),
        @ApiResponse(responseCode = "403", description = "Cannot delete the only active MFA method")
    })
    public ResponseEntity<Void> deleteMfa(
            @PathVariable @Parameter(description = "User ID", required = true) String userId,
            @PathVariable @Parameter(description = "MFA ID", required = true) String mfaId) {
        log.debug("Deleting MFA - userId: {}, mfaId: {}", userId, mfaId);
        mfaService.deleteMfa(userId, mfaId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{mfaId}/verify")
    @Operation(summary = "Verify MFA setup", 
            description = "Verify and complete the setup of a new MFA method")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "MFA verified successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid verification code"),
        @ApiResponse(responseCode = "404", description = "MFA not found")
    })
    public ResponseEntity<Void> verifyMfa(
            @PathVariable @Parameter(description = "User ID", required = true) String userId,
            @PathVariable @Parameter(description = "MFA ID", required = true) String mfaId,
            @RequestBody @Valid @Parameter(description = "Verification details", required = true)
            MfaVerificationRequest request) throws InvalidVerificationCodeException {
        log.debug("Verifying MFA setup - userId: {}, mfaId: {}", userId, mfaId);
        mfaService.verifyMfa(userId, mfaId, request.getCode());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{mfaId}/set-default")
    @Operation(summary = "Set as default MFA", 
            description = "Set a specific MFA method as the default")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Default MFA updated successfully"),
        @ApiResponse(responseCode = "404", description = "MFA not found")
    })
    public ResponseEntity<Void> setDefaultMfa(
            @PathVariable @Parameter(description = "User ID", required = true) String userId,
            @PathVariable @Parameter(description = "MFA ID", required = true) String mfaId) {
        log.debug("Setting default MFA - userId: {}, mfaId: {}", userId, mfaId);
        mfaService.setDefaultMfa(userId, mfaId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/backup-codes")
    @Operation(summary = "Generate new backup codes", 
            description = "Generate new set of backup codes for MFA recovery")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Backup codes generated successfully"),
        @ApiResponse(responseCode = "400", description = "No active MFA configured")
    })
    public ResponseEntity<BackupCodesResponse> generateBackupCodes(
            @PathVariable @Parameter(description = "User ID", required = true) String userId) {
        log.debug("Generating backup codes for user: {}", userId);
        BackupCodesResponse response = mfaService.generateBackupCodes(userId);
        return ResponseEntity.ok(response);
    }
}