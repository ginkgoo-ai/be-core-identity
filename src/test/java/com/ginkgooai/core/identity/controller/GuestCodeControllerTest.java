package com.ginkgooai.core.identity.controller;

import com.ginkgooai.core.identity.service.GuestCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GuestCodeControllerTest {

    @Mock
    private GuestCodeService guestCodeService;
    
    @InjectMocks
    private GuestCodeController controller;

    private final String resource = "shortlist";
    private final String resourceId = "resource-123";
    private final boolean write = true;
    private final String guestName = "guest";
    private final String guestEmail = "guest@example.com";
    private final String redirectUrl = "redirect-url";
    private final int expiryHours = 24;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGenerateGuestCode_Success() {
        GuestCodeController.GuestCodeRequest request = new GuestCodeController.GuestCodeRequest(
                resource, resourceId, true, guestName, guestEmail, redirectUrl, expiryHours);
        
        String guestCode = "generated-guest-code";
        when(guestCodeService.generateGuestCode(resource, resourceId, true, guestName, guestEmail, redirectUrl, expiryHours))
                .thenReturn(guestCode);
        
        ResponseEntity<GuestCodeController.GuestCodeResponse> response = 
                controller.generateGuestCode(request);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(guestCode, response.getBody().guestCode());
        assertEquals(resourceId, response.getBody().resourceId());
        assertEquals(expiryHours, response.getBody().expiryHours());
        
        assertNotNull(response.getBody().expiresAt());
        Instant expiresAt = Instant.parse(response.getBody().expiresAt());
        assertNotNull(expiresAt);
    }

    @Test
    public void testGenerateGuestCode_MissingParameters() {
        GuestCodeController.GuestCodeRequest requestWithoutResourceId = 
                new GuestCodeController.GuestCodeRequest(
                        null, null, true, null, null, redirectUrl, expiryHours);
        
        ResponseEntity<GuestCodeController.GuestCodeResponse> response = 
                controller.generateGuestCode(requestWithoutResourceId);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(guestCodeService, never()).generateGuestCode(
                any(), any(), anyBoolean(), any(), any(), any(), anyInt());
    }

    @Test
    public void testGenerateGuestCode_DefaultExpiryHours() {
        GuestCodeController.GuestCodeRequest request = new GuestCodeController.GuestCodeRequest(
                resource, resourceId, true, guestName, guestEmail, redirectUrl, 0);
        
        String guestCode = "generated-guest-code";
        when(guestCodeService.generateGuestCode(resource, resourceId, true, guestName, guestEmail, redirectUrl, 24))
                .thenReturn(guestCode);
        
        ResponseEntity<GuestCodeController.GuestCodeResponse> response = 
                controller.generateGuestCode(request);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(24, response.getBody().expiryHours());
        
        verify(guestCodeService).generateGuestCode(resource, resourceId, true, guestName, guestEmail, redirectUrl, 24);
    }

    @Test
    public void testValidateGuestCode_Success() {
        String guestCode = "valid-guest-code";
        Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);
        
        // 
        GuestCodeService.GuestCodeInfo codeInfo = new GuestCodeService.GuestCodeInfo(resource, resourceId, true, guestName, guestEmail, redirectUrl, expiresAt);
        when(guestCodeService.validateGuestCode(guestCode, resourceId))
                .thenReturn(codeInfo);

        ResponseEntity<?> response = controller.validateGuestCode(guestCode, resourceId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();

        assertTrue((Boolean) responseBody.get("valid"));
        assertEquals(resource, responseBody.get("resource"));
        assertEquals(resourceId, responseBody.get("resourceId"));
        assertEquals(guestEmail, responseBody.get("guestEmail"));
        assertEquals(write, responseBody.get("write"));
        assertEquals(expiresAt.toString(), responseBody.get("expiresAt"));
    }

    @Test
    public void testValidateGuestCode_Invalid() {
        String guestCode = "invalid-guest-code";

        OAuth2Error error = new OAuth2Error("invalid_grant", "Invalid guest code", null);
        when(guestCodeService.validateGuestCode(guestCode, resourceId))
                .thenThrow(new OAuth2AuthenticationException(error));

        ResponseEntity<?> response = controller.validateGuestCode(guestCode, resourceId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();

        assertFalse((Boolean) responseBody.get("valid"));
        assertNotNull(responseBody.get("error"));
    }
}

