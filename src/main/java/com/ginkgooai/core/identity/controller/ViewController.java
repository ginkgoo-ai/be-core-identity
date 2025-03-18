package com.ginkgooai.core.identity.controller;

import com.ginkgooai.core.identity.domain.TokenIdentity;
import com.ginkgooai.core.identity.exception.InvalidVerificationCodeException;
import com.ginkgooai.core.identity.exception.ResourceNotFoundException;
import com.ginkgooai.core.identity.service.UserService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Controller
@Slf4j
@RequestMapping("")
@Hidden
public class ViewController {

    @Value("${AUTH_CLIENT}")
    private String coreGatewayUri;

    @Autowired
    private UserService userService;

    @Autowired
    private RegisteredClientRepository clientRepository;

    @Autowired
    private OAuth2AuthorizationConsentService authorizationConsentService;

    private static final Map<String, String> SCOPE_DESCRIPTIONS = Map.of(
            "openid", "Access your basic profile information",
            "profile", "Access your profile details",
            "email", "Access your email address",
            "address", "Access your address information",
            "phone", "Access your phone number",
            "offline_access", "Access to refresh token for offline access"
    );

    @GetMapping("/")
    public String root() {
        return "redirect:" + this.coreGatewayUri; //return default OAuth Client
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
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
            model.addAttribute("autoRedirect", true);
            model.addAttribute("redirectDelay", 3000);

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

    @GetMapping("/oauth2/consent")
    public String consent(Principal principal,
                          Model model,
                          @RequestParam(OAuth2ParameterNames.CLIENT_ID) String clientId,
                          @RequestParam(OAuth2ParameterNames.SCOPE) String scope,
                          @RequestParam(OAuth2ParameterNames.STATE) String state) {

        RegisteredClient client = clientRepository.findByClientId(clientId);
        OAuth2AuthorizationConsent consent = authorizationConsentService.findById(
                client.getId(),
                principal.getName()
        );

        Set<String> requestedScopes = new HashSet<>();
        Set<String> previouslyApprovedScopes = new HashSet<>();
        Set<String> scopesToApprove = new HashSet<>();

        requestedScopes.addAll(Arrays.asList(scope.split(" ")));

        if (consent != null) {
            previouslyApprovedScopes = consent.getScopes();
        }

        for (String requestedScope : requestedScopes) {
            if (!previouslyApprovedScopes.contains(requestedScope)) {
                scopesToApprove.add(requestedScope);
            }
        }

        model.addAttribute("clientId", clientId);
        model.addAttribute("state", state);
        model.addAttribute("scopes", scopesToApprove);
        model.addAttribute("previouslyApprovedScopes", previouslyApprovedScopes);
        model.addAttribute("clientName", client.getClientName());
        model.addAttribute("scopeDescriptions", SCOPE_DESCRIPTIONS);

        return "consent";
    }
}
