package com.benwk.ginkgoocoreidentity.repository;

import com.benwk.ginkgoocoreidentity.domain.UserSocialConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSocialConnectionRepository extends JpaRepository<UserSocialConnection, Long> {
    
    Optional<UserSocialConnection> findByProviderIdAndProviderUserId(String providerId, String providerUserId);
    
}