package com.ginkgooai.core.identity.dto.response;

import com.ginkgooai.core.identity.domain.Role;
import com.ginkgooai.core.identity.domain.UserInfo;
import lombok.Data;
import lombok.Builder;

import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
public class UserResponse {
    private String id;
    private String sub;
    private String email;
    private String firstName;
    private String lastName;
    private boolean enabled;
    private Set<String> roles;

    public static UserResponse from(UserInfo user) {
        return  UserResponse.builder()
                .id(user.getId())
                .sub(user.getSocialConnections().stream().findFirst().map(sc -> sc.getProviderUserId()).orElse(null))
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .build();
    }
}