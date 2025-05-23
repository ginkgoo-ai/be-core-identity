package com.ginkgooai.core.identity.service;

import com.ginkgooai.core.common.enums.Role;
import com.ginkgooai.core.identity.domain.UserInfo;
import com.ginkgooai.core.identity.domain.UserSocialConnection;
import com.ginkgooai.core.identity.domain.enums.SocialProviderType;
import com.ginkgooai.core.identity.repository.UserRepository;
import com.ginkgooai.core.identity.repository.UserSocialConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final UserSocialConnectionRepository socialConnectionRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("OAuth2UserRequest: {}", userRequest);
        
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        SocialProviderType provider = SocialProviderType.valueOf(registrationId.toUpperCase());

        String providerUserId = extractProviderUserId(provider, oauth2User);
        String email = extractEmail(provider, oauth2User);
        String firstName = extractFirstName(provider, oauth2User);
        String lastName = extractLastName(provider, oauth2User);

        String accessToken = userRequest.getAccessToken().getTokenValue();

        String refreshToken = null;
        if (userRequest.getAdditionalParameters().containsKey("refresh_token")) {
            refreshToken = userRequest.getAdditionalParameters().get("refresh_token").toString();
        }

        createOrUpdateSocialUser(provider,
                providerUserId,
                email,
                firstName,
                lastName,
                accessToken,
                refreshToken
        );

        return oauth2User;
    }


    private String extractProviderUserId(SocialProviderType provider, OAuth2User oauth2User) {
        switch (provider) {
            case GOOGLE:
                return oauth2User.getAttribute("sub");
            case GITHUB:
                return oauth2User.getAttribute("id").toString();
            case FACEBOOK:
                return oauth2User.getAttribute("id");
            default:
                throw new IllegalArgumentException("Unsupported provider: " + provider);
        }
    }

    private String extractEmail(SocialProviderType provider, OAuth2User oauth2User) {
        return oauth2User.getAttribute("email");
    }

    private String extractFirstName(SocialProviderType provider, OAuth2User oauth2User) {
        switch (provider) {
            case GOOGLE:
                return oauth2User.getAttribute("given_name");
            case GITHUB:
                String name = oauth2User.getAttribute("name");
                return name != null ? name.split(" ")[0] : "";
            case FACEBOOK:
                return oauth2User.getAttribute("givenName");
            default:
                throw new IllegalArgumentException("Unsupported provider: " + provider);
        }
    }

    private String extractLastName(SocialProviderType provider, OAuth2User oauth2User) {
        switch (provider) {
            case GOOGLE:
                return oauth2User.getAttribute("family_name");
            case GITHUB:
                String name = oauth2User.getAttribute("name");
                String[] parts = name != null ? name.split(" ") : new String[0];
                return parts.length > 1 ? parts[parts.length - 1] : "";
            case FACEBOOK:
                return oauth2User.getAttribute("surname");
            default:
                throw new IllegalArgumentException("Unsupported provider: " + provider);
        }
    }


    @Transactional
    public UserInfo createOrUpdateSocialUser(SocialProviderType provider,
                                             String providerUserId,
                                             String email,
                                             String firstName,
                                             String lastName,
                                             String accessToken,
                                             String refreshToken) {

        Optional<UserSocialConnection> existingConnection =
                socialConnectionRepository.findByProviderIdAndProviderUserId(
                        provider.name(),
                        providerUserId
                );

        UserInfo user;
        if (existingConnection.isPresent()) {
            // Update existing user
            user = existingConnection.get().getUser();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            updateSocialConnection(existingConnection.get(), accessToken, refreshToken);
        } else {
            // Check if user exists with email
            user = userRepository.findByEmail(email).orElseGet(() -> {
                UserInfo newUser = new UserInfo();
                newUser.setEmail(email);
                newUser.setFirstName(firstName);
                newUser.setLastName(lastName);
                newUser.setRoles(new ArrayList<>());
                newUser.getRoles().add(Role.ROLE_USER.name());

                return newUser;
            });

            // Create new social connection
            UserSocialConnection socialConnection = new UserSocialConnection();
            socialConnection.setUser(user);
            socialConnection.setProviderId(provider.name());
            socialConnection.setProviderUserId(providerUserId);
            socialConnection.setAccessToken(accessToken);
            socialConnection.setRefreshToken(refreshToken);

            user.getSocialConnections().add(socialConnection);
        }

        return userRepository.save(user);
    }

    private void updateSocialConnection(
            UserSocialConnection connection,
            String accessToken,
            String refreshToken) {
        connection.setAccessToken(accessToken);
        if (refreshToken != null) {
            connection.setRefreshToken(refreshToken);
        }
        socialConnectionRepository.save(connection);
    }

}