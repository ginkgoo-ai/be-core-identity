package com.ginkgooai.core.identity.controller;

import com.ginkgooai.core.identity.service.GuestCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/guest-codes")
public class GuestCodeController {

    @Autowired
    private GuestCodeService guestCodeService;

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_guest_code.create')")
    public ResponseEntity<GuestCodeResponse> generateGuestCode(@RequestBody GuestCodeRequest request) {
        if (request.resource() == null || request.resourceId() == null || request.guestEmail() == null) {
            return ResponseEntity.badRequest().build();
        }

        int expiryHours = request.expiryHours() > 0 ? request.expiryHours() : 24;

        String guestCode = guestCodeService.generateGuestCode(
                request.resource(),
                request.resourceId(),
                request.write(),
                request.guestName(),
                request.guestEmail(),
                request.redirectUrl(),
                expiryHours
        );

        Instant expiresAt = Instant.now().plusSeconds(expiryHours * 3600L);

        return ResponseEntity.ok(new GuestCodeResponse(
                guestCode,
                request.resourceId(),
                expiresAt.toString(),
                expiryHours
        ));
    }

    @GetMapping("/validate")
    @PreAuthorize("hasAuthority('SCOPE_guest_code.validate')")
    public ResponseEntity<?> validateGuestCode(
            @RequestParam("code") String guestCode,
            @RequestParam("resource_id") String resourceId) {

        try {
            GuestCodeService.GuestCodeInfo codeInfo =
                    guestCodeService.validateGuestCode(guestCode, resourceId);

            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "resource", codeInfo.resource(),
                    "resourceId", codeInfo.resourceId(),
                    "write", codeInfo.write(),
                    "guestEmail", codeInfo.guestEmail(),
                    "expiresAt", codeInfo.expiresAt().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "error", e.getMessage()
            ));
        }
    }

    public record GuestCodeRequest(
            String resource,
            String resourceId,
            boolean write,
            String guestName,
            String guestEmail,
            String redirectUrl,
            int expiryHours
    ) {
    }

    public record GuestCodeResponse(
            String guestCode,
            String resourceId,
            String expiresAt,
            int expiryHours
    ) {
    }
}
