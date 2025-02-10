package com.ginkgooai.core.identity.domain;

import com.ginkgooai.core.identity.domain.enums.MfaStatus;
import com.ginkgooai.core.identity.domain.enums.MfaType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "mfa_info")
public class MfaInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserInfo user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private MfaStatus status = MfaStatus.DISABLED;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20)
    private MfaType type = MfaType.NONE;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "secret_key", length = 255)
    @JsonIgnore
    private String secretKey;

    @Column(name = "backup_codes", length = 1000)
    @JsonIgnore
    private String backupCodes;

    @Column(name = "recovery_email")
    private String recoveryEmail;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updatedAt;

    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    @Column(name = "attempts_count")
    private Integer attemptsCount = 0;

    @PrePersist
    protected void onCreate() {
        if (attemptsCount == null) {
            attemptsCount = 0;
        }
    }

    public boolean isEnabled() {
        return status == MfaStatus.ENABLED;
    }

    public boolean isPending() {
        return status == MfaStatus.PENDING;
    }

    public void incrementAttempts() {
        this.attemptsCount = this.attemptsCount + 1;
    }

    public void resetAttempts() {
        this.attemptsCount = 0;
    }

    public void recordSuccessfulVerification() {
        this.lastVerifiedAt = LocalDateTime.now();
        this.resetAttempts();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MfaInfo mfaInfo = (MfaInfo) o;
        return Objects.equals(id, mfaInfo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}