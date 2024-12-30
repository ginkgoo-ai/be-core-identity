package com.ginkgooai.core.identity.domain;

import com.ginkgooai.core.identity.util.StringSetConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Entity class representing an OAuth2 registered client in the authorization server.
 * This entity stores client registration details used for OAuth2 authentication and authorization.
 */
@Entity
@Table(name = "oauth2_registered_client")
@Data
public class OAuth2RegisteredClient implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();


    /**
     * Unique identifier for the registered client
     */
    @Id
    @Column(name = "id")
    private String id;

    /**
     * Client ID used for OAuth2 authentication
     */
    @Column(name = "client_id", unique = true)
    @NotBlank(message = "Client ID is required")
    private String clientId;

    /**
     * Client secret raw used for client authentication
     */
    @Column(name = "client_secret_raw")
    private String clientSecretRaw;

    /**
     * Client secret used for client authentication
     */
    @Column(name = "client_secret")
    @NotBlank(message = "Client secret is required")
    private String clientSecret;


    @Column(name = "client_id_issued_at")
    private Instant clientIdIssuedAt;

    /**
     * Timestamp when client secrets expire
     */
    @Column(name = "client_secret_expires_at")
    private Instant clientSecretExpiresAt;

    /**
     * Client name for display purposes
     */
    @Column(name = "client_name")
    @NotBlank(message = "Client name is required")
    private String clientName;

    /**
     * Client authentication methods (e.g., CLIENT_SECRET_BASIC, CLIENT_SECRET_POST)
     */
    @Column(name = "client_authentication_methods")
    @Convert(converter = StringSetConverter.class)
    private Set<String> clientAuthenticationMethods = new HashSet<>();

    /**
     * Authorization grant types supported by this client
     */
    @Column(name = "authorization_grant_types")
    @Convert(converter = StringSetConverter.class)
    private Set<String> authorizationGrantTypes = new HashSet<>();

    /**
     * Redirect URIs for authorization code flow
     */
    @Column(name = "redirect_uris")
    @Convert(converter = StringSetConverter.class)
    private Set<String> redirectUris = new HashSet<>();

    /**
     * Post-logout redirect URIs for client
     */
    @Column(name = "post_logout_redirect_uris")
    @Convert(converter = StringSetConverter.class)
    private Set<String> postLogoutRedirectUris = new HashSet<>();

    /**
     * Scopes that the client can request
     */
    @Column(name = "scopes")
    @Convert(converter = StringSetConverter.class)
    private Set<String> scopes = new HashSet<>();

    /**
     * Client settings in JSON format
     */
    @Column(name = "client_settings", columnDefinition = "TEXT")
    private String clientSettings;

    /**
     * Token settings in JSON format
     */
    @Column(name = "token_settings", columnDefinition = "TEXT")
    private String tokenSettings;

    /**
     * Creation timestamp
     */
    @Column(name = "created_at")
    private Instant createdAt;

    /**
     * Last update timestamp
     */
    @Column(name = "last_modified_at")
    private Instant lastModifiedAt;


    /**
     * Convert RegisteredClient to OAuth2RegisteredClient
     */
    public static OAuth2RegisteredClient fromRegisteredClient(RegisteredClient registeredClient) {
        OAuth2RegisteredClient client = new OAuth2RegisteredClient();
        client.setClientId(registeredClient.getClientId());
        client.setClientSecret(registeredClient.getClientSecret());
        client.setClientName(registeredClient.getClientName());

        // Convert ClientAuthenticationMethod to String Set
        client.setClientAuthenticationMethods(
                registeredClient.getClientAuthenticationMethods().stream()
                        .map(ClientAuthenticationMethod::getValue)
                        .collect(Collectors.toSet())
        );

        // Convert AuthorizationGrantType to String Set
        client.setAuthorizationGrantTypes(
                registeredClient.getAuthorizationGrantTypes().stream()
                        .map(AuthorizationGrantType::getValue)
                        .collect(Collectors.toSet())
        );

        // Convert URI Sets to String Sets
        client.setRedirectUris(registeredClient.getRedirectUris());

        client.setPostLogoutRedirectUris(registeredClient.getPostLogoutRedirectUris());

        client.setScopes(new HashSet<>(registeredClient.getScopes()));

        // Convert Settings objects to JSON strings
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            client.setClientSettings(
                    objectMapper.writeValueAsString(registeredClient.getClientSettings().getSettings())
            );
            client.setTokenSettings(
                    objectMapper.writeValueAsString(registeredClient.getTokenSettings().getSettings())
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error converting settings to JSON", e);
        }

        client.setCreatedAt(Instant.now());
        client.setLastModifiedAt(Instant.now());

        return client;
    }

    /**
     * Convert OAuth2RegisteredClient back to RegisteredClient
     */
    public RegisteredClient toRegisteredClient() {
        RegisteredClient.Builder builder = RegisteredClient.withId(getId())
                .clientId(getClientId())
                .clientSecret(getClientSecret())
                .clientName(getClientName())
                .clientIdIssuedAt(getClientIdIssuedAt())
                .clientSecretExpiresAt(getClientSecretExpiresAt());

        getClientAuthenticationMethods().forEach(method ->
                builder.clientAuthenticationMethod(new ClientAuthenticationMethod(method))
        );

        getAuthorizationGrantTypes().forEach(grant ->
                builder.authorizationGrantType(new AuthorizationGrantType(grant))
        );

        getRedirectUris().forEach(uri -> {
            try {
                builder.redirectUri(new URI(uri).toString());
            } catch (URISyntaxException e) {
                throw new IllegalStateException("Invalid redirect URI: " + uri, e);
            }
        });

        getPostLogoutRedirectUris().forEach(uri -> {
            try {
                builder.postLogoutRedirectUri(new URI(uri).toString());
            } catch (URISyntaxException e) {
                throw new IllegalStateException("Invalid post-logout redirect URI: " + uri, e);
            }
        });

        getScopes().forEach(builder::scope);

        try {
            Map<String, Object> clientSettingsMap = OBJECT_MAPPER.readValue(
                    getClientSettings(),
                    new TypeReference<>() {}
            );
            builder.clientSettings(ClientSettings.withSettings(clientSettingsMap).build());

            Map<String, Object> tokenSettingsMap = OBJECT_MAPPER.readValue(
                    getTokenSettings(),
                    new TypeReference<>() {}
            );
            builder.tokenSettings(TokenSettings.withSettings(tokenSettingsMap).build());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error parsing settings JSON", e);
        }

        return builder.build();
    }
    
    public boolean checkRedirectUrl(String url) {
        return redirectUris.contains(url);
    }

}
