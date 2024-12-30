package com.benwk.ginkgoocoreidentity.controller;

import com.benwk.ginkgoocoreidentity.domain.TokenIdentity;
import com.benwk.ginkgoocoreidentity.exception.InvalidVerificationCodeException;
import com.benwk.ginkgoocoreidentity.exception.ResourceNotFoundException;
import com.benwk.ginkgoocoreidentity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Controller
@AllArgsConstructor
@Slf4j
public class ViewController {

    private final UserService userService;

    private final RegisteredClientRepository clientRepository;

    @GetMapping("/")
    public String homePage(@RequestParam(required = false) String continueUrl, Model model) {
        return "loading";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }

        if (logout != null) {
            model.addAttribute("message", "You have been logged out");
        }

        return "login";
    }

    @GetMapping("/reset-password")
    public String resetPassword() {
        return "reset-password";
    }


    @GetMapping("/verify-email")
    public String verifyEmailByUrl(
            @RequestParam("token") String token,
            Model model) {
        try {
            // Verify email
            TokenIdentity tokenIdentity = userService.verifyEmailByToken(token);

            // Get client info and build redirect URL
            RegisteredClient client = clientRepository.findByClientId(tokenIdentity.getClientId());
            if (client != null) {
                String redirectUrl = UriComponentsBuilder
                        .fromHttpUrl(client.getRedirectUris().iterator().next())
                        .queryParam("email_verified", "true")
                        .build()
                        .toUriString();
                model.addAttribute("redirectUrl", redirectUrl);
            }

            // Set success attributes
            model.addAttribute("verified", true);
            model.addAttribute("autoRedirect", true);  // 用于控制自动跳转
            model.addAttribute("redirectDelay", 3000); // 3秒后自动跳转

            return "verify-email-result";

        } catch (InvalidVerificationCodeException e) {
            // Handle expired or invalid token
            model.addAttribute("verified", false);
            model.addAttribute("errorMessage", "Verification link has expired or is invalid. Please request a new one.");
            return "verify-email-result";

        } catch (ResourceNotFoundException e) {
            // Handle user not found
            model.addAttribute("verified", false);
            model.addAttribute("errorMessage", "Unable to find user account. Please contact support.");
            return "verify-email-result";

        } catch (Exception e) {
            // Handle unexpected errors
            log.error("Unexpected error during email verification", e);
            model.addAttribute("verified", false);
            model.addAttribute("errorMessage", "An unexpected error occurred. Please try again later or contact support.");
            return "verify-email-result";
        }
    }
}
