package com.ginkgooai.core.identity.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
@Table(
        name = "user_info",
        indexes = {
                @Index(name = "idx_email", columnList = "email", unique = true),
                @Index(name = "idx_createdat", columnList = "created_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class UserInfo implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "email", nullable = false, length = 255)
    @Email(message = "Invalid email format")
    private String email;

    @Column(name = "password", length = 255)
    @JsonIgnore
    private String password;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private UserStatus status = UserStatus.INACTIVE;

    @Column(name = "picture", length = 500)
    private String picture;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserSocialConnection> socialConnections = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MfaInfo> mfaInfos = new HashSet<>();

    @Transient
    public boolean isMfaEnabled() {
        return mfaInfos.stream()
                .anyMatch(mfa -> mfa.isEnabled() && mfa.isDefault());
    }

    @Transient
    public MfaInfo getDefaultMfa() {
        return mfaInfos.stream()
                .filter(MfaInfo::isDefault)
                .findFirst()
                .orElse(null);
    }

    @Transient
    public List<MfaInfo> getActiveMfas() {
        return mfaInfos.stream()
                .filter(MfaInfo::isEnabled)
                .collect(Collectors.toList());
    }

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updatedAt;

    @Transient
    public String getFullName() {
        return String.format("%s %s",
                StringUtils.defaultString(firstName, ""),
                StringUtils.defaultString(lastName, "")
        ).trim();
    }

    public boolean isEmailVerified() {
        return status == UserStatus.ACTIVE;
    }

    public static UserInfo from(OAuth2User user) {
        UserInfo newUser = new UserInfo();
        newUser.setEmail(user.getAttribute("email"));
        newUser.setFirstName(user.getAttribute("given_name"));
        newUser.setLastName(user.getAttribute("family_name"));
        newUser.setStatus(UserStatus.INACTIVE);
        return newUser;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserInfo userInfo = (UserInfo) o;
        return Objects.equals(email, userInfo.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isEmailVerified();
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return password;
    }
}
