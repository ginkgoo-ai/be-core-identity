package com.ginkgooai.core.identity.repository;

import com.ginkgooai.core.identity.domain.UserSocialConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSocialConnectionRepository extends JpaRepository<UserSocialConnection, Long> {
    
    Optional<UserSocialConnection> findByProviderIdAndProviderUserId(String providerId, String providerUserId);
    
}