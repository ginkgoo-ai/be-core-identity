package com.ginkgooai.core.identity.dto.response;

import com.ginkgooai.core.identity.domain.Role;
import com.ginkgooai.core.identity.domain.UserInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;

import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@Schema(description = "User response data transfer object")
public class UserResponse {
    @Schema(description = "User's unique identifier")
    private String id;

    @Schema(description = "User's subject identifier from social provider")
    private String sub;

    @Schema(description = "User's email address")
    private String email;

    @Schema(description = "User's first name")
    private String firstName;

    @Schema(description = "User's last name")
    private String lastName;
    
    @Schema(description = "Unique user name")
    private String name;

    @Schema(description = "Indicates whether user has completed their profile setup",
            example = "true")
    private boolean enabled;

    @Schema(description = "Set of user's role names",
            example = "[\"ROLE_USER\", \"ROLE_ADMIN\"]")
    private Set<String> roles;

    public static UserResponse from(UserInfo user) {
        return  UserResponse.builder()
                .id(user.getId())
                .sub(user.getSocialConnections().stream().findFirst().map(sc -> sc.getProviderUserId()).orElse(null))
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .name(user.getName())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .enabled(user.isEnabled())
                .build();
    }
}