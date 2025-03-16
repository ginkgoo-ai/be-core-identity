package com.ginkgooai.core.identity;

import com.ginkgooai.core.identity.service.GuestCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
//@Import(TestSecurityConfiguration.class)
public class GuestCodeOAuthFlowIntegrationTest {

    private final String resourceId = "resource-123";
    private final String ownerEmail = "owner@example.com";
    private final String guestEmail = "guest@example.com";
    private final String validGuestCode = "valid-guest-code";
    private final String clientId = "sharing-service";
    private final String clientSecret = "sharing-service-secret";

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private GuestCodeService guestCodeService;

    @BeforeEach
    public void setup() {
        // Setup mock for GuestCodeService
        when(guestCodeService.generateGuestCode(
                eq(resourceId), eq(ownerEmail), eq(guestEmail), anyInt()))
                .thenReturn(validGuestCode);

        when(guestCodeService.validateGuestCode(eq(validGuestCode), eq(resourceId)))
                .thenReturn(new GuestCodeService.GuestCodeInfo(
                        resourceId,
                        ownerEmail,
                        guestEmail,
                        Instant.now().plus(1, ChronoUnit.HOURS)
                ));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testGenerateGuestCode() throws Exception {
        // Test generating a guest code through the REST API
        mockMvc.perform(post("/guest-codes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "resourceId": "resource-123",
                                    "ownerEmail": "owner@example.com",
                                    "guestEmail": "guest@example.com",
                                    "expiryHours": 24
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guestCode").value(validGuestCode))
                .andExpect(jsonPath("$.resourceId").value(resourceId))
                .andExpect(jsonPath("$.expiryHours").value(24));
    }

    @Test
    public void testGuestCodeOAuthFlow() throws Exception {
        // Step 1: Generate a guest code
        MvcResult generateResult = mockMvc.perform(post("/guest-codes")
                        .with(csrf())
                        .with(request -> {
                            request.addHeader("Authorization", "Basic " +
                                    Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes()));
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "resourceId": "resource-123",
                                    "ownerEmail": "owner@example.com",
                                    "guestEmail": "guest@example.com",
                                    "expiryHours": 24
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        // Step 2: Use the guest code to get an access token
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "urn:ietf:params:oauth:grant-type:guest_code");
        params.add("guest_code", validGuestCode);
        params.add("resource_id", resourceId);

        MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
                        .with(request -> {
                            request.addHeader("Authorization", "Basic " +
                                    Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes()));
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(params))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").exists())
                .andReturn();

        // Extract the access token
        String responseContent = tokenResult.getResponse().getContentAsString();
        assertTrue(responseContent.contains("access_token"));

        // Step 3: Verify the guest code was validated
        verify(guestCodeService).validateGuestCode(validGuestCode, resourceId);
    }

    @Test
    public void testInvalidGuestCodeOAuthFlow() throws Exception {
        // Setup mock for invalid guest code
        String invalidGuestCode = "invalid-guest-code";
        when(guestCodeService.validateGuestCode(eq(invalidGuestCode), eq(resourceId)))
                .thenThrow(new OAuth2AuthenticationException(
                        new OAuth2Error("invalid_grant", "Invalid guest code", null)));

        // Try to get an access token with invalid guest code
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "urn:ietf:params:oauth:grant-type:guest_code");
        params.add("guest_code", invalidGuestCode);
        params.add("resource_id", resourceId);

        mockMvc.perform(post("/oauth2/token")
                        .with(request -> {
                            request.addHeader("Authorization", "Basic " +
                                    Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes()));
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(params))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
    }
}
