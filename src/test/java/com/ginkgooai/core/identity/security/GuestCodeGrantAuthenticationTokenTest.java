package com.ginkgooai.core.identity.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GuestCodeGrantAuthenticationTokenTest {

    @Test
    public void testConstructor_ValidParameters() {
        String guestCode = "valid-guest-code";
        String resourceId = "resource-123";
        Authentication clientPrincipal = mock(OAuth2ClientAuthenticationToken.class);
        Map<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.put("custom", "value");
        
        GuestCodeGrantAuthenticationToken token = new GuestCodeGrantAuthenticationToken(
                guestCode, resourceId, clientPrincipal, additionalParameters);
        
        assertEquals(guestCode, token.getGuestCode());
        assertEquals(resourceId, token.getResourceId());
        assertEquals(clientPrincipal, token.getPrincipal());
        assertEquals(1, token.getAdditionalParameters().size());
        assertEquals("value", token.getAdditionalParameters().get("custom"));
        assertEquals("", token.getCredentials());
        assertFalse(token.isAuthenticated());
        
        assertEquals("resource-123", token.getResourceId());
    }

    @Test
    public void testConstructor_NullAdditionalParameters() {
        String guestCode = "valid-guest-code";
        String resourceId = "resource-123";
        Authentication clientPrincipal = mock(OAuth2ClientAuthenticationToken.class);
        
        GuestCodeGrantAuthenticationToken token = new GuestCodeGrantAuthenticationToken(
                guestCode, resourceId, clientPrincipal, null);
        
        assertNotNull(token.getAdditionalParameters());
        assertTrue(token.getAdditionalParameters().isEmpty());
    }

    @Test
    public void testConstructor_InvalidParameters() {
        Authentication clientPrincipal = mock(OAuth2ClientAuthenticationToken.class);
        
        assertThrows(IllegalArgumentException.class, () -> {
            new GuestCodeGrantAuthenticationToken(
                    "", "resource-123", clientPrincipal, Collections.emptyMap());
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new GuestCodeGrantAuthenticationToken(
                    null, "resource-123", clientPrincipal, Collections.emptyMap());
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new GuestCodeGrantAuthenticationToken(
                    "valid-guest-code", "", clientPrincipal, Collections.emptyMap());
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new GuestCodeGrantAuthenticationToken(
                    "valid-guest-code", null, clientPrincipal, Collections.emptyMap());
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new GuestCodeGrantAuthenticationToken(
                    "valid-guest-code", "resource-123", null, Collections.emptyMap());
        });
    }

    @Test
    public void testAdditionalParametersImmutability() {
        String guestCode = "valid-guest-code";
        String resourceId = "resource-123";
        Authentication clientPrincipal = mock(OAuth2ClientAuthenticationToken.class);
        Map<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.put("original", "value");
        
        GuestCodeGrantAuthenticationToken token = new GuestCodeGrantAuthenticationToken(
                guestCode, resourceId, clientPrincipal, additionalParameters);
        
        additionalParameters.put("new", "value");
        
        assertEquals(1, token.getAdditionalParameters().size());
        assertEquals("value", token.getAdditionalParameters().get("original"));
        assertNull(token.getAdditionalParameters().get("new"));
        
        assertThrows(UnsupportedOperationException.class, () -> {
            token.getAdditionalParameters().put("attempt", "modify");
        });
    }
}
