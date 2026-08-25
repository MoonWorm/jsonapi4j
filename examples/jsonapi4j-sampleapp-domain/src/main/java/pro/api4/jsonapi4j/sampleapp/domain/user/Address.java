package pro.api4.jsonapi4j.sampleapp.domain.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlScopes;
import pro.api4.jsonapi4j.plugin.ac.annotation.ScopesGroup;

@Data
@AllArgsConstructor
public class Address {

    @Schema(description = "City", example = "Oslo")
    private String city;

    @AccessControl(scopes = @AccessControlScopes(@ScopesGroup("users.sensitive.read")))
    @Schema(description = "Postal code", example = "0150")
    private String zip;

}
