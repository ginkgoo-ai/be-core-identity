package com.ginkgooai.core.identity.repository;

import com.ginkgooai.core.identity.domain.OAuth2ClientRegistration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Dynamic client registration repository that supports both predefined OAuth2 providers 
 * and custom OAuth2 providers.
 * For predefined providers (like Google, GitHub, Facebook), it uses CommonOAuth2Provider configurations.
 * For custom providers, it uses fully customized configurations.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DatabaseClientRegistrationRepository implements ClientRegistrationRepository {

    private final OAuth2ClientRegistrationRepository registrationRepository;

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        log.debug("Finding client registration for id: {}", registrationId);

        Optional<OAuth2ClientRegistration> registrationOpt = registrationRepository.findByRegistrationId(registrationId);

        if (registrationOpt.isEmpty()) {
            log.warn("No client registration found for id: {}", registrationId);
            return null;
        }

        try {
            ClientRegistration registration = registrationOpt.get().toClientRegistration();
            log.info("Found client registration{}", registration);
            return registration;
        } catch (Exception e) {
            log.error("Error converting client registration for id: {}", registrationId, e);
            return null;
        }
    }

}
