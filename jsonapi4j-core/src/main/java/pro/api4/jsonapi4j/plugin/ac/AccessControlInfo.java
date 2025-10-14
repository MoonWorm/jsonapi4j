package pro.api4.jsonapi4j.plugin.ac;

import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlAccessTier;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlAuthenticated;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlOwnership;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlScopes;
import lombok.Data;

@Data
public class AccessControlInfo {

    private final AccessControlAuthenticated accessControlAuthenticated;
    private final AccessControlAccessTier accessControlAccessTier;
    private final AccessControlScopes accessControlScopes;
    private final AccessControlOwnership accessControlOwnership;

}
