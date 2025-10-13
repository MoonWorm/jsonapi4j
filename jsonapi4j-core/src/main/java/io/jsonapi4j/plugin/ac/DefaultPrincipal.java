
package io.jsonapi4j.plugin.ac;

import io.jsonapi4j.plugin.ac.tier.AccessTier;
import lombok.Data;

import java.util.Set;

@Data
public class DefaultPrincipal implements Principal {

    private final AccessTier authenticatedClientAccessTier;
    private final Set<String> authenticatedClientScopes;
    private final String authenticatedUserId;

}
