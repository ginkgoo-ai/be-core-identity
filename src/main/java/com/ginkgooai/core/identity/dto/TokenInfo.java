package com.ginkgooai.core.identity.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
public class TokenInfo {
    private String id;
    private String username;
    private String clientId;
    private String tokenType;
    private Instant issuedAt;
    private Instant expiresAt;
    private Set<String> scopes;
}

