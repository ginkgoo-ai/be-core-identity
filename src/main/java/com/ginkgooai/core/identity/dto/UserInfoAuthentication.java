package com.ginkgooai.core.identity.dto;

import com.ginkgooai.core.identity.domain.UserInfo;
import com.ginkgooai.core.identity.domain.UserStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class UserInfoAuthentication implements UserDetails {
    private String id;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private UserStatus status;
    private List<String> roles = new ArrayList<>();

    public static UserInfoAuthentication from(UserInfo userInfo) {
        UserInfoAuthentication auth = new UserInfoAuthentication();
        auth.setId(userInfo.getId());
        auth.setEmail(userInfo.getEmail());
        auth.setPassword(userInfo.getPassword());
        auth.setFirstName(userInfo.getFirstName());
        auth.setLastName(userInfo.getLastName());
        auth.setStatus(userInfo.getStatus());
        auth.setRoles(userInfo.getRoles());
        return auth;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
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
        return status == UserStatus.ACTIVE;
    }
}