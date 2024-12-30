package com.benwk.ginkgoocoreidentity.repository;

import com.benwk.ginkgoocoreidentity.domain.OAuth2RegisteredClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OAuth2RegisteredClientRepository extends JpaRepository<OAuth2RegisteredClient, String> {
    Optional<OAuth2RegisteredClient> findByClientId(String clientId);
}
