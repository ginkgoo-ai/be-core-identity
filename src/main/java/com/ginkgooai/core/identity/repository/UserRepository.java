package com.ginkgooai.core.identity.repository;

import com.ginkgooai.core.identity.domain.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserInfo, String>, JpaSpecificationExecutor<UserInfo> {
    
    Optional<UserInfo> findByEmail(String email);
    
    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE UserInfo u SET " +
            "u.picture = COALESCE(:#{#user.picture}, u.picture), " +
            "u.name = COALESCE(:#{#user.name}, u.name) " +
            "WHERE u.id = :#{#user.id}")
    void updateSelective(@Param("user") UserInfo user);
}