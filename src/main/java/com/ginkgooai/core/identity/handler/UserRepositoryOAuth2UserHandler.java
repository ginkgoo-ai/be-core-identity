package com.ginkgooai.core.identity.handler;

import com.ginkgooai.core.common.enums.Role;
import com.ginkgooai.core.identity.domain.UserInfo;
import com.ginkgooai.core.identity.domain.UserSocialConnection;
import com.ginkgooai.core.identity.domain.UserStatus;
import com.ginkgooai.core.identity.domain.enums.LoginMethod;
import com.ginkgooai.core.identity.repository.UserRepository;
import com.ginkgooai.core.identity.repository.UserSocialConnectionRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class UserRepositoryOAuth2UserHandler implements Consumer<OAuth2User> {
	private final UserRepository userRepository;
	private final UserSocialConnectionRepository socialConnectionRepository;

	@Override
	@Transactional
	public void accept(OAuth2User oauth2User) {
		log.debug("Processing OAuth2User: {}", oauth2User);

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication instanceof OAuth2AuthenticationToken)) {
			throw new IllegalStateException("Unexpected authentication type");
		}

		OAuth2AuthenticationToken oauth2Authentication = (OAuth2AuthenticationToken) authentication;
		String registrationId = oauth2Authentication.getAuthorizedClientRegistrationId();

		try {
			Map<String, Object> attributes = oauth2User.getAttributes();
			SocialUserInfo userInfo = extractUserInfo(registrationId, oauth2User, attributes);
			syncUserData(userInfo);
		} catch (Exception e) {
			log.error("Failed to sync user data: {}", e.getMessage(), e);
			throw new OAuth2AuthenticationException("Failed to synchronize user data");
		}
	}

	@Data
	@Builder
	private static class SocialUserInfo {
		private String providerId;
		private String providerUserId;
		private String providerUsername;
		private String email;
		private String firstName;
		private String lastName;
		private boolean emailVerified;
		private String picture;
		private String accessToken;
		private String refreshToken;
		private LocalDateTime tokenExpiresAt;
	}


	private SocialUserInfo extractUserInfo(String registrationId, OAuth2User oauth2User,
										   Map<String, Object> attributes) {
		return switch (registrationId.toLowerCase()) {
			case "google" -> extractGoogleUserInfo(oauth2User, attributes);
//			case "github" -> extractGithubUserInfo(oauth2User, attributes);
			default -> throw new IllegalArgumentException("Unsupported provider: " + registrationId);
		};
	}

	private SocialUserInfo extractGoogleUserInfo(OAuth2User oauth2User,
												 Map<String, Object> attributes) {
		return SocialUserInfo.builder()
				.providerId("google")
				.providerUserId(oauth2User.getAttribute("sub"))
				.providerUsername(oauth2User.getAttribute("email"))
				.email(oauth2User.getAttribute("email"))
				.firstName(oauth2User.getAttribute("given_name"))
				.lastName(oauth2User.getAttribute("family_name"))
				.emailVerified(Boolean.TRUE.equals(oauth2User.getAttribute("email_verified")))
				.picture(oauth2User.getAttribute("picture"))
				.tokenExpiresAt(getExpirationFromAttributes(attributes))
				.build();
	}

	private LocalDateTime getExpirationFromAttributes(Map<String, Object> attributes) {
		if (attributes.containsKey("exp")) {
			Instant expiration = (Instant) attributes.get("exp");
			return LocalDateTime.ofInstant(expiration, ZoneId.systemDefault());
		}
		return null;
	}

	@Transactional
	protected void syncUserData(SocialUserInfo userInfo) {
		Optional<UserSocialConnection> existingConnection = socialConnectionRepository
				.findByProviderIdAndProviderUserId(
						userInfo.getProviderId(),
						userInfo.getProviderUserId()
				);

		UserInfo user;
		if (existingConnection.isPresent()) {
			user = existingConnection.get().getUser();
			updateUserInfo(user, userInfo);
			updateSocialConnection(existingConnection.get(), userInfo);
		} else {
			user = userRepository.findByEmail(userInfo.getEmail())
					.orElseGet(() -> createNewUser(userInfo));

			createSocialConnection(user, userInfo);
		}

		userRepository.save(user);
	}

	private UserInfo createNewUser(SocialUserInfo userInfo) {
		UserInfo user = new UserInfo();
		user.setEmail(userInfo.getEmail());
		user.setFirstName(userInfo.getFirstName());
		user.setLastName(userInfo.getLastName());
		user.setStatus(UserStatus.INACTIVE);
		user.setLoginMethods(new ArrayList<>());
		user.getLoginMethods().add(LoginMethod.OAUTH.name());
		user.setRoles(Collections.singletonList(Role.ROLE_USER.name()));

		return userRepository.save(user);
	}

	private void updateUserInfo(UserInfo user, SocialUserInfo userInfo) {
		if (userInfo.getFirstName() != null) {
			user.setFirstName(userInfo.getFirstName());
		}
		if (userInfo.getLastName() != null) {
			user.setLastName(userInfo.getLastName());
		}
		if (userInfo.getPicture() != null) {
			user.setPicture(userInfo.getPicture());
		}
	}

	private void updateSocialConnection(UserSocialConnection connection, SocialUserInfo userInfo) {
		connection.setProviderUsername(userInfo.getProviderUsername());
		connection.setAccessToken(userInfo.getAccessToken());
		connection.setRefreshToken(userInfo.getRefreshToken());
		connection.setTokenExpiresAt(userInfo.getTokenExpiresAt());

		socialConnectionRepository.save(connection);
	}

	private void createSocialConnection(UserInfo user, SocialUserInfo userInfo) {
		UserSocialConnection connection = new UserSocialConnection();
		connection.setUser(user);
		connection.setProviderId(userInfo.getProviderId());
		connection.setProviderUserId(userInfo.getProviderUserId());
		connection.setProviderUsername(userInfo.getProviderUsername());
		connection.setAccessToken(userInfo.getAccessToken());
		connection.setRefreshToken(userInfo.getRefreshToken());
		connection.setTokenExpiresAt(userInfo.getTokenExpiresAt());

		user.getSocialConnections().add(connection);
		socialConnectionRepository.save(connection);
	}

}