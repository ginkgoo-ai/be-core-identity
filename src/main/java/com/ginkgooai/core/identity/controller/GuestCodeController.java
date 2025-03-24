package com.ginkgooai.core.identity.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ginkgooai.core.identity.service.GuestCodeService;

@RestController
@RequestMapping("/guest-codes")
public class GuestCodeController {

        @Autowired
        private GuestCodeService guestCodeService;

        @PostMapping
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
                                expiryHours,
                                request.workspaceId());

                Instant expiresAt = Instant.now().plusSeconds(expiryHours * 3600L);

                return ResponseEntity.ok(new GuestCodeResponse(
                                guestCode,
                                request.resourceId(),
                                expiresAt.toString(),
                                expiryHours));
        }

        @GetMapping("/validate")
        public ResponseEntity<?> validateGuestCode(
                        @RequestParam("code") String guestCode,
                        @RequestParam("resource_id") String resourceId) {

                try {
                        GuestCodeService.GuestCodeInfo codeInfo = guestCodeService.validateGuestCode(guestCode,
                                        resourceId);

                        Map<String, Object> responseMap = new HashMap<>();
                        responseMap.put("valid", true);
                        responseMap.put("resource", codeInfo.resource());
                        responseMap.put("resourceId", codeInfo.resourceId());
                        responseMap.put("write", codeInfo.write());
                        responseMap.put("guestEmail", codeInfo.guestEmail());
                        responseMap.put("expiresAt", codeInfo.expiresAt().toString());

                        if (codeInfo.workspaceId() != null) {
                                responseMap.put("workspaceId", codeInfo.workspaceId());
                        }

                        return ResponseEntity.ok(responseMap);
                } catch (Exception e) {
                        return ResponseEntity.ok(Map.of(
                                        "valid", false,
                                        "error", e.getMessage()));
                }
        }

        public record GuestCodeRequest(
                        String resource,
                        String resourceId,
                        boolean write,
                        String guestName,
                        String guestEmail,
                        String redirectUrl,
                        int expiryHours,
                        String workspaceId) {
        }

        public record GuestCodeResponse(
                        String guestCode,
                        String resourceId,
                        String expiresAt,
                        int expiryHours) {
        }
}
