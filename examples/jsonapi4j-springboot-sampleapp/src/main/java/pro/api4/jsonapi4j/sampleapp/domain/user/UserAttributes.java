package pro.api4.jsonapi4j.sampleapp.domain.user;

import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlOwnership;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlScopes;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Data
public class UserAttributes {

    @Schema(description = "First name", example = "John", requiredMode = REQUIRED)
    private final String firstName;

    @Schema(description = "Last name", example = "Doe", requiredMode = REQUIRED)
    private final String lastName;

    @Schema(description = "Email", example = "john@doe.com", requiredMode = REQUIRED)
    private final String email;

    @AccessControlScopes(requiredScopes = "user.read")
    @AccessControlOwnership(ownerIdFieldPath = "id")
    @Schema(description = "User's credit card number", example = "123456789", requiredMode = REQUIRED)
    private final String creditCardNumber;

}
