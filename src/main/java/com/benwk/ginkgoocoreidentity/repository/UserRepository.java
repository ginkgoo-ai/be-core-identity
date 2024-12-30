package com.benwk.ginkgoocoreidentity.repository;

import com.benwk.ginkgoocoreidentity.domain.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserInfo, String> {
    
    Optional<UserInfo> findByEmail(String email);
    
    boolean existsByEmail(String email);
}