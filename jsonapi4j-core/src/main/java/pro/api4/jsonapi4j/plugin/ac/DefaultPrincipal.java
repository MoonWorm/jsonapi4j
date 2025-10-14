
package pro.api4.jsonapi4j.plugin.ac;

import pro.api4.jsonapi4j.plugin.ac.tier.AccessTier;
import lombok.Data;

import java.util.Set;

@Data
public class DefaultPrincipal implements Principal {

    private final AccessTier authenticatedClientAccessTier;
    private final Set<String> authenticatedClientScopes;
    private final String authenticatedUserId;

}
