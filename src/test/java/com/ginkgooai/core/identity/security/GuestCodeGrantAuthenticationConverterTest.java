package com.ginkgooai.core.identity.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GuestCodeGrantAuthenticationConverterTest {

    private GuestCodeGrantAuthenticationConverter converter;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private SecurityContext securityContext;
    
    @Mock
    private OAuth2ClientAuthenticationToken clientAuthentication;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        converter = new GuestCodeGrantAuthenticationConverter();
        
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(clientAuthentication);
    }

    @Test
    public void testConvert_Success() {
        when(request.getParameter("grant_type")).thenReturn("urn:ietf:params:oauth:grant-type:guest_code");
        when(request.getParameter("guest_code")).thenReturn("valid-guest-code");
        when(request.getParameter("resource_id")).thenReturn("resource-123");
        when(request.getParameterMap()).thenReturn(Map.of(
                "grant_type", new String[]{"urn:ietf:params:oauth:grant-type:guest_code"},
                "guest_code", new String[]{"valid-guest-code"},
                "resource_id", new String[]{"resource-123"},
                "custom_param", new String[]{"custom-value"}
        ));
        
        Authentication result = converter.convert(request);
        
        assertNotNull(result);
        assertTrue(result instanceof GuestCodeGrantAuthenticationToken);
        
        GuestCodeGrantAuthenticationToken token = (GuestCodeGrantAuthenticationToken) result;
        assertEquals("valid-guest-code", token.getGuestCode());
        assertEquals("resource-123", token.getResourceId());
        assertEquals(clientAuthentication, token.getPrincipal());
        
        Map<String, Object> additionalParams = token.getAdditionalParameters();
        assertEquals(1, additionalParams.size());
        assertEquals("custom-value", additionalParams.get("custom_param"));
    }

    @Test
    public void testConvert_WrongGrantType() {
        when(request.getParameter("grant_type")).thenReturn("authorization_code");
        
        Authentication result = converter.convert(request);
        
        assertNull(result);
    }

    @Test
    public void testConvert_MissingGuestCode() {
        when(request.getParameter("grant_type")).thenReturn("urn:ietf:params:oauth:grant-type:guest_code");
        when(request.getParameter("resource_id")).thenReturn("resource-123");
        when(request.getParameterMap()).thenReturn(Map.of(
                "grant_type", new String[]{"urn:ietf:params:oauth:grant-type:guest_code"},
                "resource_id", new String[]{"resource-123"}
        ));
        
        assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(request));
    }

    @Test
    public void testConvert_MissingResourceId() {
        when(request.getParameter("grant_type")).thenReturn("urn:ietf:params:oauth:grant-type:guest_code");
        when(request.getParameter("guest_code")).thenReturn("valid-guest-code");
        when(request.getParameterMap()).thenReturn(Map.of(
                "grant_type", new String[]{"urn:ietf:params:oauth:grant-type:guest_code"},
                "guest_code", new String[]{"valid-guest-code"}
        ));
        
        assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(request));
    }

    @Test
    public void testConvert_MultipleGuestCodeValues() {
        when(request.getParameter("grant_type")).thenReturn("urn:ietf:params:oauth:grant-type:guest_code");
        when(request.getParameter("guest_code")).thenReturn("valid-guest-code");
        when(request.getParameter("resource_id")).thenReturn("resource-123");
        when(request.getParameterMap()).thenReturn(Map.of(
                "grant_type", new String[]{"urn:ietf:params:oauth:grant-type:guest_code"},
                "guest_code", new String[]{"valid-guest-code", "another-code"},
                "resource_id", new String[]{"resource-123"}
        ));
        
        assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(request));
    }
}
