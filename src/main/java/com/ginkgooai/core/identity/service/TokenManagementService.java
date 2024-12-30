package com.ginkgooai.core.identity.service;

import com.ginkgooai.core.identity.dto.TokenInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class TokenManagementService {

    private final CachedOAuth2AuthorizationService authorizationService;

    public TokenManagementService(CachedOAuth2AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    /**
     * Revoke all tokens for a specific user
     */
    public void revokeTokens(String username) {
        try {
            Collection<OAuth2Authorization> authorizations =
                    authorizationService.findByPrincipalName(username);

            authorizations.forEach(auth -> authorizationService.remove(auth));
            log.info("Successfully revoked all tokens for user: {}", username);
        } catch (Exception e) {
            log.error("Failed to revoke tokens for user: " + username, e);
            throw new RuntimeException("Error getting active tokens", e);
        }
    }

    /**
     * Revoke tokens for specific client and user
     */
    public void revokeClientTokens(String username, String clientId) {
        try {
            Collection<OAuth2Authorization> authorizations =
                    authorizationService.findByPrincipalName(username);

            authorizations.stream()
                    .filter(auth -> auth.getRegisteredClientId().equals(clientId))
                    .forEach(auth -> authorizationService.remove(auth));

            log.info("""
                Successfully revoked tokens for user: {} 
                and client: {}
                """,
                    username,
                    clientId
            );
        } catch (Exception e) {
            log.error("""
                Failed to revoke tokens for user: {} 
                and client: {}
                """,
                    username,
                    clientId,
                    e
            );
            throw new RuntimeException("Error getting active tokens", e);
        }
    }

    /**
     * Get all tokens for a specific user
     */
    public List<TokenInfo> getUserTokens(String username) {
        try {
            Collection<OAuth2Authorization> authorizations =
                    authorizationService.findByPrincipalName(username);

            return authorizations.stream()
                    .map(this::convertToTokenInfo)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get tokens for user: " + username, e);
            throw new RuntimeException("Error getting active tokens", e);
        }
    }

    /**
     * Get all active tokens with pagination
     */
    public Page<TokenInfo> getAllActiveTokens(Pageable pageable) {
        try {
            return authorizationService.findAllValid(pageable)
                    .map(this::convertToTokenInfo);
        } catch (Exception e) {
            log.error("Failed to get active tokens", e);
            throw new RuntimeException("Error getting active tokens", e);
        }
    }

    private TokenInfo convertToTokenInfo(OAuth2Authorization authorization) {
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken =
                authorization.getToken(OAuth2AccessToken.class);

        return TokenInfo.builder()
                .id(authorization.getId())
                .username(authorization.getPrincipalName())
                .clientId(authorization.getRegisteredClientId())
                .tokenType(accessToken.getToken().getTokenType().getValue())
                .issuedAt(accessToken.getToken().getIssuedAt())
                .expiresAt(accessToken.getToken().getExpiresAt())
                .scopes(accessToken.getToken().getScopes())
                .build();
    }
}
