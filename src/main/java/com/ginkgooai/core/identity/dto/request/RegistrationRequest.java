package com.ginkgooai.core.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(name = "RegistrationRequest", description = "Request object for user registration")
public class RegistrationRequest {

//    @Schema(
//            title = "Client id",
//            description = "SPA's client id",
//            example = "31ba3a02-ec9a-4ef7-9a52-113e54d4fa56",
//            requiredMode = Schema.RequiredMode.REQUIRED
//    )
//    @NotBlank(message = "Last name is required")
//    private String clientId;

    @Schema(
        title = "Email",
        description = "User's email address",
        example = "user@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @Schema(
        title = "Online Registration",
        description = "Indicates if the user registered online",
        example = "true"
    )
    private boolean onlineRegistration;

    @Schema(
        title = "Password",
        description = "User's password - must be between 12 and 64 characters",
        example = "Password1234",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minLength = 8,
        maxLength = 32
    )
    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{12,64}$",
        message = "Password must be at least 12 characters long and contain at least one digit, " +
            "one lowercase letter, and one uppercase letter")
    private String password;

    @Schema(
        title = "First Name",
        description = "User's first name",
        example = "John"
    )
    private String firstName;

    @Schema(
        title = "User Name",
        description = "User's name",
        example = "John Smith"
    )
    private String name;

    @Schema(
        title = "Last Name",
        description = "User's last name",
        example = "Doe"
    )
    private String lastName;
}
