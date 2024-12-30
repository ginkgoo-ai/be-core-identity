package com.ginkgooai.core.identity.dto.request;

import com.ginkgooai.core.identity.domain.OAuth2ClientRegistration;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.oauth2.core.AuthenticationMethod;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data Transfer Object for OAuth2 client registration requests.
 * Used for creating and updating OAuth2 client registrations.
 */
@Data
public class OAuth2ClientRegistrationDto implements Serializable {
    @NotBlank(message = "Registration ID is required")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Registration ID can only contain letters, numbers, underscores and hyphens")
    private String registrationId;

    @NotBlank(message = "Client ID is required")
    private String clientId;

    @NotBlank(message = "Client Secret is required")
    private String clientSecret;

    @NotBlank(message = "Authorization Grant Type is required")
    private String authorizationGrantType;

    private String redirectUri;

    @NotBlank(message = "Scope is required")
    private String scope;

    @NotBlank(message = "Authorization URI is required")
    private String authorizationUri;

    @NotBlank(message = "Token URI is required")
    private String tokenUri;

    @NotBlank(message = "User Info URI is required")
    private String userInfoUri;

    @NotBlank(message = "User Name Attribute is required")
    private String userNameAttribute;

    @Size(min = 1, max = 100, message = "Client name must be between 1 and 100 characters")
    private String clientName;


    public RegisteredClient toRegisteredClient() {
        Set<String> scopes = Arrays.stream(this.scope.split("\\s+"))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        return RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(this.clientId)
                .clientSecret(this.clientSecret)
                .clientName(this.clientName)
                .clientIdIssuedAt(Instant.now())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(new AuthorizationGrantType(this.authorizationGrantType))
                .redirectUri(this.redirectUri)
                .scopes(scopeSet -> scopeSet.addAll(scopes))
                .build();
    }

    /**
     * Convert DTO to OAuth2ClientRegistration entity
     * @return OAuth2ClientRegistration entity
     */
    public OAuth2ClientRegistration toEntity() {
        OAuth2ClientRegistration registration = new OAuth2ClientRegistration();

        // Set basic properties
        registration.setRegistrationId(this.registrationId);
        registration.setClientId(this.clientId);
        registration.setClientSecret(this.clientSecret);
        registration.setClientName(this.clientName);

        // Set URIs
        registration.setRedirectUri(this.redirectUri);
        registration.setAuthorizationUri(this.authorizationUri);
        registration.setTokenUri(this.tokenUri);
        registration.setUserInfoUri(this.userInfoUri);
        registration.setAuthorizationGrantType(this.authorizationGrantType);
        registration.setUserNameAttributeName(this.userNameAttribute);

        // Convert scope string to Set<String>
        if (this.scope != null && !this.scope.isEmpty()) {
            Set<String> scopes = Arrays.stream(this.scope.split("\\s+"))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
            registration.setScopes(scopes);
        } else {
            registration.setScopes(new HashSet<>());
        }

        // Set default values for optional fields
        registration.setUserInfoAuthenticationMethod(AuthenticationMethod.HEADER.getValue()); 

        return registration;
    }

    /**
     * Create DTO from entity
     * @param registration OAuth2ClientRegistration entity
     * @return OAuth2ClientRegistrationDto
     */
    public static OAuth2ClientRegistrationDto fromEntity(OAuth2ClientRegistration registration) {
        OAuth2ClientRegistrationDto dto = new OAuth2ClientRegistrationDto();

        dto.setRegistrationId(registration.getRegistrationId());
        dto.setClientId(registration.getClientId());
        dto.setClientSecret(registration.getClientSecret());
        dto.setClientName(registration.getClientName());
        dto.setRedirectUri(registration.getRedirectUri());
        dto.setAuthorizationUri(registration.getAuthorizationUri());
        dto.setTokenUri(registration.getTokenUri());
        dto.setUserInfoUri(registration.getUserInfoUri());
        dto.setAuthorizationGrantType(registration.getAuthorizationGrantType());
        dto.setUserNameAttribute(registration.getUserNameAttributeName());

        // Convert Set<String> to space-delimited scope string
        if (registration.getScopes() != null && !registration.getScopes().isEmpty()) {
            dto.setScope(String.join(" ", registration.getScopes()));
        }

        return dto;
    }

}
