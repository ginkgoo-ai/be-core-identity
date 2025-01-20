package com.ginkgooai.core.identity.controller;

import com.ginkgooai.core.identity.dto.request.CreateClientRequest;
import com.ginkgooai.core.identity.dto.response.RegisteredClientDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/oauth2/clients")
@RequiredArgsConstructor
@Tag(name = "OAuth2 Client Management", description = "APIs for managing OAuth2 client applications")
@SecurityRequirement(name = "bearerAuth")
public class OAuth2ClientController {

    private final RegisteredClientRepository clientRepository;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new OAuth2 client", description = "Creates a new OAuth2 client with the specified configuration. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Client created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RegisteredClientDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Client ID already exists",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @ResponseStatus(HttpStatus.CREATED)
    public RegisteredClientDTO createClient(@RequestBody @Valid @Parameter(description = "Client creation request", required = true) CreateClientRequest request) {
        log.debug("Creating new OAuth2 client with ID: {}", request.getClientId());

        if (clientRepository.findByClientId(request.getClientId()) != null) {
            log.warn("Attempt to create client with existing ID: {}", request.getClientId());
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format("Client ID '%s' already exists", request.getClientId()));
        }

        try {
            RegisteredClient client = buildRegisteredClient(request);
            clientRepository.save(client);
            log.info("Successfully created OAuth2 client with ID: {}", request.getClientId());
            return RegisteredClientDTO.from(clientRepository.findByClientId(request.getClientId()));
        } catch (Exception e) {
            log.error("Failed to create OAuth2 client with ID: {}", request.getClientId(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create OAuth2 client", e);
        }
    }

    @GetMapping(value = "/{clientId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get OAuth2 client details", description = "Retrieves the details of an OAuth2 client by client ID. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client details retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RegisteredClientDTO.class))),
            @ApiResponse(responseCode = "404", description = "Client not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public RegisteredClientDTO getClient(@Parameter(description = "Client ID", required = true) @PathVariable @NotBlank String clientId) {
        log.debug("Retrieving OAuth2 client with ID: {}", clientId);

        RegisteredClient client = clientRepository.findByClientId(clientId);
        if (client == null) {
            log.warn("OAuth2 client not found with ID: {}", clientId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Client with ID '%s' not found", clientId));
        }

        log.debug("Successfully retrieved OAuth2 client with ID: {}", clientId);
        return RegisteredClientDTO.from(client);
    }

    @DeleteMapping("/{clientId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete OAuth2 client", description = "Deletes an OAuth2 client by client ID. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Client deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Client not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "501", description = "Delete operation not implemented",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClient(@Parameter(description = "Client ID", required = true) @PathVariable @NotBlank String clientId) {
        log.debug("Attempting to delete OAuth2 client with ID: {}", clientId);

        if (clientRepository.findByClientId(clientId) == null) {
            log.warn("Attempt to delete non-existent OAuth2 client with ID: {}", clientId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Client with ID '%s' not found", clientId));
        }

        // TODO: Implement delete operation
        log.warn("Delete operation not implemented for OAuth2 client with ID: {}", clientId);
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Delete operation not yet implemented");
    }

    private RegisteredClient buildRegisteredClient(CreateClientRequest request) {
        log.debug("Building RegisteredClient from request for client ID: {}", request.getClientId());

        return RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(request.getClientId())
                .clientSecret(request.getClientSecret())
                .clientName(request.getClientName())
                .clientAuthenticationMethods(methods -> {
                    request.getAuthenticationMethods().forEach(method -> methods.add(new ClientAuthenticationMethod(method)));
                })
                .authorizationGrantTypes(types -> {
                    request.getGrantTypes().forEach(type -> types.add(new AuthorizationGrantType(type)));
                })
                .redirectUris(uris -> uris.addAll(request.getRedirectUris()))
                .scopes(scopes -> scopes.addAll(request.getScopes()))
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(request.isRequireAuthorizationConsent())
                        .requireProofKey(request.isRequireProofKey())
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofSeconds(request.getAccessTokenTtl()))
                        .refreshTokenTimeToLive(Duration.ofSeconds(request.getRefreshTokenTtl()))
                        .reuseRefreshTokens(request.isReuseRefreshTokens())
                        .build())
                .build();
    }
}