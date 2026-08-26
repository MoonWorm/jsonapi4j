package pro.api4.jsonapi4j.sampleapp.domain.user;

import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlOwnership;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlScopes;
import pro.api4.jsonapi4j.plugin.ac.annotation.ScopesGroup;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import pro.api4.jsonapi4j.plugin.ac.annotation.Authenticated;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@AccessControl(authenticated = Authenticated.AUTHENTICATED)
@Data
public class UserAttributes {

    @Schema(description = "First and last name together", example = "John Smith", requiredMode = REQUIRED)
    private final String fullName;

    @Schema(description = "Email", example = "john@doe.com", requiredMode = REQUIRED)
    private final String email;

    @AccessControl(
            scopes = @AccessControlScopes(@ScopesGroup("users.sensitive.read")),
            ownership = @AccessControlOwnership(ownerIdFieldPath = "id")
    )
    @Schema(description = "User's credit card number", example = "123456789", requiredMode = REQUIRED)
    private final String creditCardNumber;

    @ArraySchema(
            arraySchema = @Schema(description = "Known addresses. Send an empty array to remove them all."),
            schema = @Schema(
                    implementation = HomeAddress.class,
                    description = "An address. Carrying a door code makes it a home address, which is "
                            + "reflected in the response; omit it for an ordinary one."))
    private final List<Address> addresses;

}
