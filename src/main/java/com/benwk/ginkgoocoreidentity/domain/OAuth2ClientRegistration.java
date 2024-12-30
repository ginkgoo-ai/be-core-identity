package com.benwk.ginkgoocoreidentity.domain;

import com.benwk.ginkgoocoreidentity.domain.enums.RegistrationStatus;
import com.benwk.ginkgoocoreidentity.domain.enums.SocialProviderType;
import com.benwk.ginkgoocoreidentity.util.StringSetConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;


/**
 * Entity class representing OAuth2 client registration configuration in a client application.
 * Stores configuration for connecting to OAuth2 providers (e.g., Google, GitHub, custom IdPs).
 */
@Entity
@Table(name = "oauth2_client_registration")
@Data
public class OAuth2ClientRegistration implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique identifier for the registration (e.g., "google", "github")
     */
    @Id
    @Column(name = "registration_id")
    private String registrationId;

    /**
     * Client ID issued by the OAuth2 provider
     */
    @Column(name = "client_id", nullable = false)
    @NotBlank(message = "Client ID is required")
    private String clientId;

    /**
     * Client secret issued by the OAuth2 provider
     */
    @Column(name = "client_secret", nullable = false)
    @NotBlank(message = "Client secret is required")
    private String clientSecret;

    /**
     * OAuth2 authorization grant type
     */
    @Column(name = "authorization_grant_type", nullable = false)
    @NotBlank(message = "Authorization grant type is required")
    private String authorizationGrantType = "authorization_code";

    /**
     * Redirect URI for authorization code flow
     */
    @Column(name = "redirect_uri")
    @NotBlank(message = "Redirect URI is required")
    private String redirectUri;

    /**
     * OAuth2 provider's authorization endpoint URI
     */
    @Column(name = "authorization_uri")
    private String authorizationUri;

    /**
     * OAuth2 provider's token endpoint URI
     */
    @Column(name = "token_uri", nullable = false)
    @NotBlank(message = "Token URI is required")
    private String tokenUri;

    /**
     * OAuth2 provider's user info endpoint URI
     */
    @Column(name = "user_info_uri")
    private String userInfoUri;

    /**
     * OAuth2 provider's user info authentication method
     */
    @Column(name = "user_info_authentication_method")
    private String userInfoAuthenticationMethod;

    /**
     * User name attribute name in the OAuth2 provider's response
     */
    @Column(name = "user_name_attribute_name")
    private String userNameAttributeName;

    /**
     * OAuth2 provider's JWK set URI for signature verification
     */
    @Column(name = "jwk_set_uri")
    private String jwkSetUri;

    /**
     * OAuth2 provider's issuer URI
     */
    @Column(name = "issuer_uri")
    private String issuerUri;

    /**
     * Configured scopes for this registration
     */
    @Column(name = "scopes")
    @Convert(converter = StringSetConverter.class)
    private Set<String> scopes = new HashSet<>();

    /**
     * Display name for the OAuth2 provider
     */
    @Column(name = "provider_display_name")
    private String providerDisplayName;

    /**
     * Provider type (e.g., GOOGLE, GITHUB, CUSTOM)
     */
    @Column(name = "provider_type")
    @Enumerated(EnumType.STRING)
    private SocialProviderType providerType;

    /**
     * Client authentication method
     */
    @Column(name = "client_authentication_method")
    private String clientAuthenticationMethod = "client_secret_basic";

    /**
     * Creation timestamp
     */
    @Column(name = "created_at")
    private Instant createdAt;

    /**
     * Last update timestamp
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Status of the registration
     */
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private RegistrationStatus status = RegistrationStatus.ACTIVE;

    /**
     * Client name for display purposes
     */
    @Column(name = "client_name")
    @NotBlank(message = "Client name is required")
    private String clientName;

    public ClientRegistration toClientRegistration() {
        return ClientRegistration.withRegistrationId(registrationId)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(new ClientAuthenticationMethod(clientAuthenticationMethod))
                .authorizationGrantType(new AuthorizationGrantType(authorizationGrantType))
                .redirectUri(redirectUri)
                .scope(scopes)
                .authorizationUri(authorizationUri)
                .tokenUri(tokenUri)
                .jwkSetUri(jwkSetUri)
                .issuerUri(issuerUri)
                .userInfoUri(userInfoUri)
                .userNameAttributeName(userNameAttributeName)
                .clientName(clientName)
                .build();
    }
}
