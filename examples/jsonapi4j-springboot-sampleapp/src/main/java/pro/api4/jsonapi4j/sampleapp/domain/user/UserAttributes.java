package pro.api4.jsonapi4j.sampleapp.domain.user;

import pro.api4.jsonapi4j.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.ac.annotation.AccessControlOwnership;
import pro.api4.jsonapi4j.ac.annotation.AccessControlScopes;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Data
public class UserAttributes {

    @Schema(description = "First and last name together", example = "John Smith", requiredMode = REQUIRED)
    private final String fullName;

    @Schema(description = "Email", example = "john@doe.com", requiredMode = REQUIRED)
    private final String email;

    @AccessControl(
            scopes = @AccessControlScopes(requiredScopes = "user.read"),
            ownership = @AccessControlOwnership(ownerIdFieldPath = "id")
    )
    @Schema(description = "User's credit card number", example = "123456789", requiredMode = REQUIRED)
    private final String creditCardNumber;

}
