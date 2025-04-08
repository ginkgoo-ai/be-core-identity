package com.ginkgooai.core.identity.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_info")
@EntityListeners(AuditingEntityListener.class)
public class UserInfo extends BaseAuditableEntity implements UserDetails {

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

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "login_methods", columnDefinition = "varchar[]")
    private List<String> loginMethods;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "name", length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private UserStatus status = UserStatus.INACTIVE;

    @Column(name = "picture", length = 500)
    private String picture;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserSocialConnection> socialConnections = new HashSet<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "roles", columnDefinition = "varchar[]")
    private List<String> roles;

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
        if (roles == null || roles.size() == 0) {
            return Collections.emptyList();
        }

        return roles.stream()
            .map(role -> new SimpleGrantedAuthority(role))
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

    public String getName() {
        return String.join(" ", firstName, lastName);
    }
}
