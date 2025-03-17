package com.ginkgooai.core.identity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class GuestCodeServiceRedisTest {

    @Mock
    private RedisTemplate<String, GuestCodeService.GuestCodeInfo> redisTemplate;

    @Mock
    private ValueOperations<String, GuestCodeService.GuestCodeInfo> valueOperations;

    @InjectMocks
    private GuestCodeService guestCodeService;

    private final String resourceId = "resource-123";
    private final String ownerEmail = "owner@example.com";
    private final String guestEmail = "guest@example.com";
    private final int expiryHours = 24;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        // 不要在setup中设置通用存根，而是在每个需要的测试方法中单独设置
    }

    @Test
    public void testGenerateGuestCode() {
        // 在这个测试中设置需要的存根
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Execute the method
        String guestCode = guestCodeService.generateGuestCode(
                resourceId, ownerEmail, guestEmail, expiryHours);

        // Verify the code is not empty
        assertNotNull(guestCode);
        assertTrue(guestCode.length() > 0);

        // Verify Redis operations were called
        verify(valueOperations).set(
                eq("guest_code:" + guestCode),
                any(GuestCodeService.GuestCodeInfo.class),
                eq((long)expiryHours),
                eq(TimeUnit.HOURS));
    }

    @Test
    public void testValidateGuestCode_Success() {
        // Setup test data
        String guestCode = "valid-guest-code";
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);

        GuestCodeService.GuestCodeInfo codeInfo = new GuestCodeService.GuestCodeInfo(
                resourceId, ownerEmail, guestEmail, expiresAt);

        // Setup Redis mock
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("guest_code:" + guestCode)).thenReturn(codeInfo);

        // Execute the method
        GuestCodeService.GuestCodeInfo result = guestCodeService.validateGuestCode(guestCode, resourceId);

        // Verify the result
        assertEquals(resourceId, result.resourceId());
        assertEquals(ownerEmail, result.ownerEmail());
        assertEquals(guestEmail, result.guestEmail());
        assertEquals(expiresAt, result.expiresAt());

    }

    @Test
    public void testValidateGuestCode_InvalidCode() {
        // Setup Redis mock to return null (invalid code)
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        // Execute and verify exception
        OAuth2AuthenticationException exception = assertThrows(
                OAuth2AuthenticationException.class,
                () -> guestCodeService.validateGuestCode("invalid-code", resourceId)
        );

        assertEquals("invalid_grant", exception.getError().getErrorCode());
        assertTrue(exception.getMessage().contains("Invalid guest code"));
    }

    @Test
    public void testValidateGuestCode_ResourceIdMismatch() {
        // Setup test data
        String guestCode = "valid-guest-code";
        String wrongResourceId = "wrong-resource";
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);

        GuestCodeService.GuestCodeInfo codeInfo = new GuestCodeService.GuestCodeInfo(
                resourceId, ownerEmail, guestEmail, expiresAt);

        // Setup Redis mock
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("guest_code:" + guestCode)).thenReturn(codeInfo);

        // Execute and verify exception
        OAuth2AuthenticationException exception = assertThrows(
                OAuth2AuthenticationException.class,
                () -> guestCodeService.validateGuestCode(guestCode, wrongResourceId)
        );

        assertEquals("invalid_grant", exception.getError().getErrorCode());
        assertTrue(exception.getMessage().contains("Resource ID mismatch"));
    }

    @Test
    public void testValidateGuestCode_ExpiredCode() {
        // Setup test data with expired time
        String guestCode = "expired-guest-code";
        Instant expiresAt = Instant.now().minus(1, ChronoUnit.HOURS);

        GuestCodeService.GuestCodeInfo codeInfo = new GuestCodeService.GuestCodeInfo(
                resourceId, ownerEmail, guestEmail, expiresAt);

        // Setup Redis mock
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("guest_code:" + guestCode)).thenReturn(codeInfo);

        // Execute and verify exception
        OAuth2AuthenticationException exception = assertThrows(
                OAuth2AuthenticationException.class,
                () -> guestCodeService.validateGuestCode(guestCode, resourceId)
        );

        assertEquals("invalid_grant", exception.getError().getErrorCode());
        assertTrue(exception.getMessage().contains("Guest code has expired"));

        // Verify expired code was deleted
        verify(redisTemplate).delete("guest_code:" + guestCode);
    }

    @Test
    public void testValidateGuestCode_MissingParameters() {
        // 这个测试不需要设置任何存根，因为它会在参数验证阶段就失败

        // Test with null guest code
        OAuth2AuthenticationException exception1 = assertThrows(
                OAuth2AuthenticationException.class,
                () -> guestCodeService.validateGuestCode(null, resourceId)
        );

        assertEquals("invalid_grant", exception1.getError().getErrorCode());

        // Test with empty guest code
        OAuth2AuthenticationException exception2 = assertThrows(
                OAuth2AuthenticationException.class,
                () -> guestCodeService.validateGuestCode("", resourceId)
        );

        assertEquals("invalid_grant", exception2.getError().getErrorCode());

        // Test with null resource ID
        OAuth2AuthenticationException exception3 = assertThrows(
                OAuth2AuthenticationException.class,
                () -> guestCodeService.validateGuestCode("valid-code", null)
        );

        assertEquals("invalid_grant", exception3.getError().getErrorCode());
    }
}
