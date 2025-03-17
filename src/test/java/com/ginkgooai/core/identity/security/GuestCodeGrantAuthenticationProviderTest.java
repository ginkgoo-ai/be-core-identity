package com.ginkgooai.core.identity.security;

import com.ginkgooai.core.identity.service.GuestCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class GuestCodeGrantAuthenticationProviderTest {

    private GuestCodeGrantAuthenticationProvider provider;
    
    @Mock
    private OAuth2AuthorizationService authorizationService;
    
    @Mock
    private OAuth2TokenGenerator<OAuth2Token> tokenGenerator;
    
    @Mock
    private GuestCodeService guestCodeService;
    
    @Mock
    private OAuth2ClientAuthenticationToken clientPrincipal;
    
    @Mock
    private RegisteredClient registeredClient;
    
    @Mock
    private AuthorizationServerContext authorizationServerContext;
    
    @Mock
    private OAuth2Token generatedToken;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        provider = new GuestCodeGrantAuthenticationProvider(
                authorizationService, tokenGenerator, guestCodeService);
        
        AuthorizationServerContextHolder.setContext(authorizationServerContext);
        
        when(clientPrincipal.getRegisteredClient()).thenReturn(registeredClient);
        when(clientPrincipal.isAuthenticated()).thenReturn(true);
        
        Set<AuthorizationGrantType> grantTypes = new HashSet<>();
        grantTypes.add(new AuthorizationGrantType("urn:ietf:params:oauth:grant-type:guest_code"));
        when(registeredClient.getAuthorizationGrantTypes()).thenReturn(grantTypes);
        when(registeredClient.getScopes()).thenReturn(Set.of("read"));
    }

    @Test
    public void testAuthenticate_Success() {
        String guestCode = "valid-guest-code";
        String resourceId = "resource-123";
        String ownerEmail = "owner@example.com";
        String guestEmail = "guest@example.com";
        Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);
        
        GuestCodeGrantAuthenticationToken authenticationToken = new GuestCodeGrantAuthenticationToken(
                guestCode, resourceId, clientPrincipal, Collections.emptyMap());
        
        GuestCodeService.GuestCodeInfo codeInfo = new GuestCodeService.GuestCodeInfo(
                resourceId, ownerEmail, guestEmail, expiresAt);
        when(guestCodeService.validateGuestCode(guestCode, resourceId)).thenReturn(codeInfo);
        
        when(tokenGenerator.generate(any(OAuth2TokenContext.class))).thenReturn(generatedToken);
        when(generatedToken.getTokenValue()).thenReturn("token-value");
        
        Authentication result = provider.authenticate(authenticationToken);
        
        assertNotNull(result);
        assertTrue(result instanceof OAuth2AccessTokenAuthenticationToken);
        
        OAuth2AccessTokenAuthenticationToken accessTokenResult = 
                (OAuth2AccessTokenAuthenticationToken) result;
        assertEquals(registeredClient, accessTokenResult.getRegisteredClient());
        assertEquals(clientPrincipal, accessTokenResult.getPrincipal());
        
        OAuth2AccessToken accessToken = accessTokenResult.getAccessToken();
        assertEquals("token-value", accessToken.getTokenValue());
        assertEquals(OAuth2AccessToken.TokenType.BEARER, accessToken.getTokenType());
        assertEquals(expiresAt, accessToken.getExpiresAt());
        
        verify(authorizationService).save(any(OAuth2Authorization.class));
    }

    @Test
    public void testAuthenticate_UnauthenticatedClient() {
        when(clientPrincipal.isAuthenticated()).thenReturn(false);
        
        GuestCodeGrantAuthenticationToken authenticationToken = new GuestCodeGrantAuthenticationToken(
                "valid-guest-code", "resource-123", clientPrincipal, Collections.emptyMap());
        
        assertThrows(OAuth2AuthenticationException.class, () -> provider.authenticate(authenticationToken));
    }

    @Test
    public void testAuthenticate_UnauthorizedGrantType() {
        when(registeredClient.getAuthorizationGrantTypes()).thenReturn(Collections.emptySet());
        
        GuestCodeGrantAuthenticationToken authenticationToken = new GuestCodeGrantAuthenticationToken(
                "valid-guest-code", "resource-123", clientPrincipal, Collections.emptyMap());
        
        OAuth2AuthenticationException exception = assertThrows(
                OAuth2AuthenticationException.class, 
                () -> provider.authenticate(authenticationToken));
        
        assertEquals("unauthorized_client", exception.getError().getErrorCode());
    }

    @Test
    public void testAuthenticate_InvalidGuestCode() {
        String guestCode = "invalid-guest-code";
        String resourceId = "resource-123";
        
        GuestCodeGrantAuthenticationToken authenticationToken = new GuestCodeGrantAuthenticationToken(
                guestCode, resourceId, clientPrincipal, Collections.emptyMap());
        
        when(guestCodeService.validateGuestCode(guestCode, resourceId))
                .thenThrow(new OAuth2AuthenticationException("invalid_grant"));
        
        assertThrows(OAuth2AuthenticationException.class, () -> provider.authenticate(authenticationToken));
    }

    @Test
    public void testAuthenticate_TokenGenerationFailure() {
        String guestCode = "valid-guest-code";
        String resourceId = "resource-123";
        String ownerEmail = "owner@example.com";
        String guestEmail = "guest@example.com";
        Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);
        
        GuestCodeGrantAuthenticationToken authenticationToken = new GuestCodeGrantAuthenticationToken(
                guestCode, resourceId, clientPrincipal, Collections.emptyMap());
        
        GuestCodeService.GuestCodeInfo codeInfo = new GuestCodeService.GuestCodeInfo(
                resourceId, ownerEmail, guestEmail, expiresAt);
        when(guestCodeService.validateGuestCode(guestCode, resourceId)).thenReturn(codeInfo);
        
        when(tokenGenerator.generate(any(OAuth2TokenContext.class))).thenReturn(null);
        
        OAuth2AuthenticationException exception = assertThrows(
                OAuth2AuthenticationException.class, 
                () -> provider.authenticate(authenticationToken));
        
        assertEquals("server_error", exception.getError().getErrorCode());
    }

    @Test
    public void testSupports() {
        assertTrue(provider.supports(GuestCodeGrantAuthenticationToken.class));
        
        assertFalse(provider.supports(OAuth2ClientAuthenticationToken.class));
    }
}
