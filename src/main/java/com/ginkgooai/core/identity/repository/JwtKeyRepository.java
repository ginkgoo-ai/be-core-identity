package com.ginkgooai.core.identity.repository;

import com.ginkgooai.core.identity.domain.JwtKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JwtKeyRepository extends JpaRepository<JwtKey, String> {
    Optional<JwtKey> findFirstByOrderByCreatedAtDesc();
}