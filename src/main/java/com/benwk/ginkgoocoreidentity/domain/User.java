package com.benwk.ginkgoocoreidentity.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;

@Data
@Entity
@Table(name = "users")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String email;
    private String picture;
    private String provider; 
    private String providerId;
    
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(255)")
    private Role role;

    private boolean enabled = true;

    public enum Role {
        USER, ADMIN
    }

    public boolean hasRole(Role role) {
        return this.role == role;
    }

    public boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return null;
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
        return enabled;
    }

    public static User fromOAuth2User(OAuth2User oauth2User, String provider) {
        User user = new User();
        user.setEmail(oauth2User.getAttribute("email"));
        user.setName(oauth2User.getAttribute("name"));
        user.setPicture(oauth2User.getAttribute("picture"));
        user.setProvider(provider);
        user.setProviderId(oauth2User.getAttribute("sub")); // Google使用sub作为用户ID
        return user;
    }

    public void updateFromOAuth2User(OAuth2User oauth2User) {
        this.setName(oauth2User.getAttribute("name"));
        this.setPicture(oauth2User.getAttribute("picture"));
    }
}