package pro.api4.jsonapi4j.sampleapp.domain.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlScopes;
import pro.api4.jsonapi4j.plugin.ac.annotation.ScopesGroup;

@Data
@EqualsAndHashCode(callSuper = true)
public class HomeAddress extends Address {

    @AccessControl(scopes = @AccessControlScopes(@ScopesGroup("users.sensitive.read")))
    @Schema(description = "Door code", example = "1234")
    private String doorCode;

    public HomeAddress(String city, String zip, String doorCode) {
        super(city, zip);
        this.doorCode = doorCode;
    }

}
