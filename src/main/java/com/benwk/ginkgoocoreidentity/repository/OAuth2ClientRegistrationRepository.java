package com.benwk.ginkgoocoreidentity.repository;

import com.benwk.ginkgoocoreidentity.domain.OAuth2ClientRegistration;
import com.benwk.ginkgoocoreidentity.domain.enums.RegistrationStatus;
import com.benwk.ginkgoocoreidentity.domain.enums.SocialProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OAuth2ClientRegistrationRepository extends JpaRepository<OAuth2ClientRegistration, String> {
    
    Optional<OAuth2ClientRegistration> findByRegistrationId(String registrationId);
    
    List<OAuth2ClientRegistration> findByProviderType(SocialProviderType providerType);
    
    boolean existsByClientId(String clientId);

    @Modifying
    @Query("UPDATE OAuth2ClientRegistration o SET o.status = :status WHERE o.registrationId = :registrationId")
    int updateStatusByRegistrationId(@Param("registrationId") String registrationId, @Param("status") RegistrationStatus status);
}
