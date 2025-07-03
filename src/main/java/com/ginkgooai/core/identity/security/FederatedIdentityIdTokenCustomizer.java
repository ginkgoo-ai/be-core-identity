package com.ginkgooai.core.identity.security;

import com.ginkgooai.core.identity.dto.response.UserResponse;
import com.ginkgooai.core.identity.service.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.HashMap;
import java.util.Map;

/**
 * Customizes ID Token and Access Token by replacing Google's sub with local
 * user ID
 * and adding necessary claims
 */
public final class FederatedIdentityIdTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

	private final UserService userService;

	public FederatedIdentityIdTokenCustomizer(UserService userService) {
		this.userService = userService;
	}

	@Override
	public void customize(JwtEncodingContext context) {
		if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue()) ||
			OAuth2TokenType.ACCESS_TOKEN.getValue().equals(context.getTokenType().getValue())) {

			Map<String, Object> claims = extractClaims(context.getPrincipal());

			// Safe email extraction with null check
			final String email;
			if (claims.containsKey("email") && claims.get("email") != null) {
				email = claims.get("email").toString();
			}
			else {
				// Skip customization for unsupported auth types
				return;
			}

			UserResponse localUser = userService.loadUser(email);
			if (localUser == null) {
				return; // Skip if user not found
			}
			
			context.getClaims().claims(existingClaims -> {
				existingClaims.put("sub", localUser.getId()); //replace social login sub(Google) with local user ID
				existingClaims.put("name", localUser.getName());
				existingClaims.put("email", email);
				existingClaims.put("role", localUser.getRoles());
				existingClaims.put("enabled", localUser.isEnabled());
				if (claims.get("workspace_id") != null) {
					existingClaims.put("workspace_id", claims.get("workspace_id"));
				}
			});
		}
	}

	/**
	 * Extracts claims from the authentication principal
	 *
	 * @param principal The authentication principal
	 * @return Map of claims
	 */
	private Map<String, Object> extractClaims(Authentication principal) {
		if (principal.getPrincipal() instanceof OidcUser) {
			return ((OidcUser) principal.getPrincipal()).getIdToken().getClaims();
		} else if (principal.getPrincipal() instanceof OAuth2User) {
			return ((OAuth2User) principal.getPrincipal()).getAttributes();
		} else if (principal instanceof ShareCodeGrantAuthenticationToken) {
			return ((ShareCodeGrantAuthenticationToken) principal).extractClaims();
		}
		else if (principal instanceof UsernamePasswordAuthenticationToken) {
			// Handle form login - extract email from UserDetails
			Object userDetails = principal.getPrincipal();
			Map<String, Object> claims = new HashMap<>();

			if (userDetails instanceof UserDetails) {
				// For form login, username is typically the email
				claims.put("email", ((UserDetails) userDetails).getUsername());
			}

			return claims;
		}
		return new HashMap<>();
	}
}
