package io.jsonapi4j.plugin.ac;

import io.jsonapi4j.plugin.ac.annotation.AccessControlAccessTier;
import io.jsonapi4j.plugin.ac.annotation.AccessControlAuthenticated;
import io.jsonapi4j.plugin.ac.annotation.AccessControlOwnership;
import io.jsonapi4j.plugin.ac.annotation.AccessControlScopes;
import lombok.Data;

@Data
public class AccessControlInfo {

    private final AccessControlAuthenticated accessControlAuthenticated;
    private final AccessControlAccessTier accessControlAccessTier;
    private final AccessControlScopes accessControlScopes;
    private final AccessControlOwnership accessControlOwnership;

}
