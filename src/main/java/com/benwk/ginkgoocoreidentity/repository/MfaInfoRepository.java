package com.benwk.ginkgoocoreidentity.repository;

import com.benwk.ginkgoocoreidentity.domain.MfaInfo;
import com.benwk.ginkgoocoreidentity.domain.enums.MfaStatus;
import com.benwk.ginkgoocoreidentity.domain.enums.MfaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MfaInfoRepository extends JpaRepository<MfaInfo, String> {
   
   /**
    * Find all MFA methods for a specific user
    * @param userId User's ID
    * @return List of MFA methods
    */
   List<MfaInfo> findByUserId(String userId);
   
   /**
    * Find specific MFA type for a user
    * @param userId User's ID
    * @param type MFA type
    * @return Optional MFA info
    */
   Optional<MfaInfo> findByUserIdAndType(String userId, MfaType type);
   
   /**
    * Find default MFA method for a user
    * @param userId User's ID
    * @return Optional MFA info
    */
   Optional<MfaInfo> findByUserIdAndIsDefaultTrue(String userId);
   
   /**
    * Count user's MFA methods
    * @param userId User's ID
    * @return Number of MFA methods
    */
   long countByUserId(String userId);
   
   /**
    * Count user's MFA methods with specific status
    * @param userId User's ID
    * @param status MFA status
    * @return Number of MFA methods
    */
   long countByUserIdAndStatus(String userId, MfaStatus status);
   
   /**
    * Clear default flag for all user's MFA methods
    * @param userId User's ID
    */
   @Modifying
   @Query("UPDATE MfaInfo m SET m.isDefault = false WHERE m.user.id = :userId")
   void clearDefaultMfaForUser(@Param("userId") String userId);
   
   /**
    * Delete all MFA methods for a user
    * @param userId User's ID
    */
   @Modifying
   @Query("DELETE FROM MfaInfo m WHERE m.user.id = :userId")
   void deleteAllByUserId(@Param("userId") String userId);
   
   /**
    * Find all MFA methods that need cleanup (old and unverified)
    * @param status MFA status to check
    * @param createdBefore Timestamp to check against
    * @return List of MFA infos
    */
   @Query("SELECT m FROM MfaInfo m WHERE m.status = :status AND m.createdAt < :createdBefore")
   List<MfaInfo> findUnverifiedMfaOlderThan(
           @Param("status") MfaStatus status, 
           @Param("createdBefore") LocalDateTime createdBefore
   );
   
   /**
    * Update attempts count
    * @param mfaId MFA ID
    * @param attempts New attempts count
    */
   @Modifying
   @Query("UPDATE MfaInfo m SET m.attemptsCount = :attempts WHERE m.id = :mfaId")
   void updateAttemptsCount(@Param("mfaId") String mfaId, @Param("attempts") Integer attempts);
   
   /**
    * Find all locked MFA methods (exceeded max attempts)
    * @param maxAttempts Maximum allowed attempts
    * @return List of locked MFA infos
    */
   @Query("SELECT m FROM MfaInfo m WHERE m.attemptsCount >= :maxAttempts")
   List<MfaInfo> findLockedMfa(@Param("maxAttempts") Integer maxAttempts);
}