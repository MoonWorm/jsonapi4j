package pro.api4.jsonapi4j.sampleapp.domain.user;

import pro.api4.jsonapi4j.plugin.ac.impl.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.impl.annotation.AccessControlOwnership;
import pro.api4.jsonapi4j.plugin.ac.impl.annotation.AccessControlScopes;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import pro.api4.jsonapi4j.plugin.ac.impl.annotation.Authenticated;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@AccessControl(authenticated = Authenticated.AUTHENTICATED)
@Data
public class UserAttributes {

    @Schema(description = "First and last name together", example = "John Smith", requiredMode = REQUIRED)
    private final String fullName;

    @Schema(description = "Email", example = "john@doe.com", requiredMode = REQUIRED)
    private final String email;

    @AccessControl(
            scopes = @AccessControlScopes(requiredScopes = "users.sensitive.read"),
            ownership = @AccessControlOwnership(ownerIdFieldPath = "id")
    )
    @Schema(description = "User's credit card number", example = "123456789", requiredMode = REQUIRED)
    private final String creditCardNumber;

}
