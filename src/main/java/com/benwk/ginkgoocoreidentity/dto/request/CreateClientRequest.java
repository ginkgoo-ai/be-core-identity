package com.benwk.ginkgoocoreidentity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class CreateClientRequest {
    @NotBlank
    private String clientId;
    
    @NotBlank
    private String clientSecret;
    
    @NotBlank
    private String clientName;
    
    @NotEmpty
    private Set<String> authenticationMethods = Set.of("none");
    
    @NotEmpty
    private Set<String> grantTypes = Set.of("authorization_code");
    
    @NotEmpty
    private Set<String> redirectUris;
    
    @NotEmpty
    private Set<String> scopes;
    
    private boolean requireAuthorizationConsent = false;
    
    private boolean requireProofKey = false;
    
    private long accessTokenTtl = 3600;  // 1 hour
    
    private long refreshTokenTtl= 2592000;  // 30 days
    
    private boolean reuseRefreshTokens = true;
}