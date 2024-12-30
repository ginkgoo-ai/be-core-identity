package com.benwk.ginkgoocoreidentity.controller;

import com.benwk.ginkgoocoreidentity.domain.OAuth2ClientRegistration;
import com.benwk.ginkgoocoreidentity.domain.enums.RegistrationStatus;
import com.benwk.ginkgoocoreidentity.exception.ResourceNotFoundException;
import com.benwk.ginkgoocoreidentity.repository.OAuth2ClientRegistrationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/oauth2/registrations")
@RequiredArgsConstructor
@Validated
@Tag(name = "OAuth2 Client Registration Management",
        description = "APIs for managing OAuth2 client registrations in the identity service")
@SecurityRequirement(name = "bearerAuth")
public class OAuth2RegistrationController {

    private final OAuth2ClientRegistrationRepository clientRegistrationRepository;

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Create new OAuth2 client registration",
            description = "Creates a new OAuth2 client registration with the provided details. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Client registration created successfully",
                    content = @Content(schema = @Schema(implementation = OAuth2ClientRegistration.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "409", description = "Client registration already exists")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OAuth2ClientRegistration> createRegistration(
            @Valid @RequestBody OAuth2ClientRegistration registration) {
        log.debug("Creating new OAuth2 client registration: {}", registration.getClientId());

        // Check if registration already exists
        if (clientRegistrationRepository.existsById(registration.getRegistrationId())) {
            log.warn("Client registration already exists: {}", registration.getRegistrationId());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        OAuth2ClientRegistration saved = clientRegistrationRepository.save(registration);
        log.info("Created OAuth2 client registration: {}", saved.getRegistrationId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping(
            value = "/{registrationId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Update existing OAuth2 client registration",
            description = "Updates an existing OAuth2 client registration. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Client registration updated successfully"),
            @ApiResponse(responseCode = "404", description = "Client registration not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OAuth2ClientRegistration> updateRegistration(
            @Parameter(description = "Registration ID", required = true)
            @PathVariable @NotBlank String registrationId,
            @Valid @RequestBody OAuth2ClientRegistration registration) {
        log.debug("Updating OAuth2 client registration: {}", registrationId);

        if (!clientRegistrationRepository.existsById(registrationId)) {
            throw new ResourceNotFoundException("Client registration", "registrationId", registrationId);
        }

        // Ensure IDs match
        if (!registrationId.equals(registration.getRegistrationId())) {
            throw new IllegalArgumentException("Path registration ID does not match body registration ID");
        }

        OAuth2ClientRegistration updated = clientRegistrationRepository.save(registration);
        log.info("Updated OAuth2 client registration: {}", registrationId);
        return ResponseEntity.ok(updated);
    }

    @GetMapping(
            value = "/{registrationId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get OAuth2 client registration by ID",
            description = "Retrieves an OAuth2 client registration by its ID. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Client registration found"),
            @ApiResponse(responseCode = "404", description = "Client registration not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OAuth2ClientRegistration> getRegistration(
            @Parameter(description = "Registration ID", required = true)
            @PathVariable @NotBlank String registrationId) {
        log.debug("Fetching OAuth2 client registration: {}", registrationId);
        return clientRegistrationRepository.findById(registrationId)
                .map(registration -> {
                    log.debug("Found OAuth2 client registration: {}", registrationId);
                    return ResponseEntity.ok(registration);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Client registration", "registrationId", registrationId));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get all OAuth2 client registrations",
            description = "Retrieves all OAuth2 client registrations. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of client registrations retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OAuth2ClientRegistration>> getAllRegistrations() {
        log.debug("Fetching all OAuth2 client registrations");
        List<OAuth2ClientRegistration> registrations = clientRegistrationRepository.findAll();
        log.debug("Found {} OAuth2 client registrations", registrations.size());
        return ResponseEntity.ok(registrations);
    }

    @DeleteMapping("/{registrationId}")
    @Operation(
            summary = "Delete OAuth2 client registration",
            description = "Deletes an OAuth2 client registration by its ID. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Client registration deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Client registration not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRegistration(
            @Parameter(description = "Registration ID", required = true)
            @PathVariable @NotBlank String registrationId) {
        log.debug("Deleting OAuth2 client registration: {}", registrationId);

        if (!clientRegistrationRepository.existsById(registrationId)) {
            throw new ResourceNotFoundException("Client registration", "registrationId", registrationId);
        }

        clientRegistrationRepository.deleteById(registrationId);
        log.info("Deleted OAuth2 client registration: {}", registrationId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(
            value = "/{registrationId}/status",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Update OAuth2 client registration status",
            description = "Updates the status of an OAuth2 client registration. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "404", description = "Client registration not found"),
            @ApiResponse(responseCode = "400", description = "Invalid status value"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Integer> updateStatus(
            @Parameter(description = "Registration ID", required = true)
            @PathVariable @NotBlank String registrationId,
            @Parameter(description = "New registration status", required = true)
            @RequestParam RegistrationStatus status) {
        log.debug("Updating status for OAuth2 client registration: {} to {}", registrationId, status);

        if (!clientRegistrationRepository.existsById(registrationId)) {
            throw new ResourceNotFoundException("Client registration", "registrationId", registrationId);
        }

        int updated = clientRegistrationRepository.updateStatusByRegistrationId(registrationId, status);
        log.info("Updated status for OAuth2 client registration: {} to {}", registrationId, status);
        return ResponseEntity.ok(updated);
    }
}