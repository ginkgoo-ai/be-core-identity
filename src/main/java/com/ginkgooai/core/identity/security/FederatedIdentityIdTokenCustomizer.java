package com.ginkgooai.core.identity.security;

import com.ginkgooai.core.common.security.CustomGrantTypes;
import com.ginkgooai.core.identity.dto.response.UserResponse;
import com.ginkgooai.core.identity.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.HashMap;
import java.util.Map;

/**
 * Customizes ID Token and Access Token by replacing Google's sub with local user ID
 * and adding necessary claims
 */
public final class FederatedIdentityIdTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

	private final UserService userService;

	public FederatedIdentityIdTokenCustomizer(UserService userService) {
		this.userService = userService;
	}

	@Override
	public void customize(JwtEncodingContext context) {
		// guest_code grant type 
		if (context.getAuthorizationGrantType() == CustomGrantTypes.GUEST_CODE) {
			GuestCodeGrantAuthenticationToken principal = context.getPrincipal();;
			context.getClaims().claims(existingClaims -> {
				existingClaims.put("email", principal.getEmail());
				existingClaims.put("name", principal.getUserName());
				existingClaims.put("role", principal.getAuthorities().stream().findFirst().get().getAuthority());
			});
		} else if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue()) ||
				OAuth2TokenType.ACCESS_TOKEN.getValue().equals(context.getTokenType().getValue())) {

			Map<String, Object> claims = extractClaims(context.getPrincipal());
			String email = claims.get("email").toString();

			UserResponse localUser = userService.loadUser(email);
			context.getClaims().claims(existingClaims -> {
				// Store original Google sub
				String googleSub = existingClaims.get("sub").toString();

				// Replace sub with local user ID and keep original as google_sub
				existingClaims.put("google_sub", googleSub);
				existingClaims.put("sub", localUser.getId());
				existingClaims.put("name", localUser.getName());
				existingClaims.put("email", email);
				existingClaims.put("role", localUser.getRoles());
			});
		}
	}

	/**
	 * Extracts claims from the authentication principal
	 * @param principal The authentication principal
	 * @return Map of claims
	 */
	private Map<String, Object> extractClaims(Authentication principal) {
		if (principal.getPrincipal() instanceof OidcUser) {
			return ((OidcUser) principal.getPrincipal()).getIdToken().getClaims();
		} else if (principal.getPrincipal() instanceof OAuth2User) {
			return ((OAuth2User) principal.getPrincipal()).getAttributes();
		}
		return new HashMap<>();
	}
}
