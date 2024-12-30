package com.benwk.ginkgoocoreidentity.dto.response;

import com.benwk.ginkgoocoreidentity.domain.Role;
import com.benwk.ginkgoocoreidentity.domain.UserInfo;
import lombok.Data;
import lombok.Builder;

import java.util.Set;

@Data
@Builder
public class UserResponse {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private boolean enabled;
    private Set<Role> roles;

    public static UserResponse from(UserInfo user) {
        return  UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(user.getRoles())
                .build();
    }
}