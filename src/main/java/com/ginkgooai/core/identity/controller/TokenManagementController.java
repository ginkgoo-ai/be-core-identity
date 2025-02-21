package com.ginkgooai.core.identity.controller;

import com.ginkgooai.core.identity.dto.TokenInfo;
import com.ginkgooai.core.identity.service.TokenManagementService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/oauth2/tokens")
@PreAuthorize("hasRole('ADMIN')")  // Requires ADMIN role for all endpoints
@Slf4j
@Hidden
public class TokenManagementController {

    private final TokenManagementService tokenService;

    public TokenManagementController(TokenManagementService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * Get all active tokens with pagination
     */
    @GetMapping
    public ResponseEntity<Page<TokenInfo>> getAllActiveTokens(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(tokenService.getAllActiveTokens(pageable));
    }

    /**
     * Get all tokens for a specific user
     */
    @GetMapping("/user/{username}")
    public ResponseEntity<List<TokenInfo>> getUserTokens(
            @PathVariable String username) {
        return ResponseEntity.ok(tokenService.getUserTokens(username));
    }

    /**
     * Revoke all tokens for a specific user
     */
    @DeleteMapping("/user/{username}")
    public ResponseEntity<Void> revokeUserTokens(
            @PathVariable String username) {
        tokenService.revokeTokens(username);
        return ResponseEntity.ok().build();
    }

    /**
     * Revoke tokens for specific client and user
     */
    @DeleteMapping("/user/{username}/client/{clientId}")
    public ResponseEntity<Void> revokeClientTokens(
            @PathVariable String username,
            @PathVariable String clientId) {
        tokenService.revokeClientTokens(username, clientId);
        return ResponseEntity.ok().build();
    }
}
