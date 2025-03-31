package com.ginkgooai.core.identity.dto.response;

import com.ginkgooai.core.common.enums.Role;
import com.ginkgooai.core.identity.domain.UserInfo;
import com.ginkgooai.core.identity.domain.enums.LoginMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
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

	@Schema(description = "Logo file")
	private String picture;

	@Schema(description = "Set of user's role names",
		example = "[\"ROLE_USER\", \"ROLE_ADMIN\"]")
	private List<String> roles;

	@Schema(description = "List of login methods used by the user",
		example = "[\"TEMP_TOKEN\", \"MFA\"]")
	private List<String> loginMethods;

	public static UserResponse from(UserInfo user) {
		return UserResponse.builder()
			.id(user.getId())
			.sub(user.getSocialConnections().stream().findFirst().map(sc -> sc.getProviderUserId()).orElse(null))
			.email(user.getEmail())
			.firstName(user.getFirstName())
			.lastName(user.getLastName())
			.name(user.getName())
			.picture(user.getPicture())
			.roles(user.getRoles().stream().map(Role::name).collect(Collectors.toList()))
			.loginMethods(user.getLoginMethods().stream().map(LoginMethod::name).collect(Collectors.toList()))
			.enabled(user.isEnabled())
			.build();
	}
}