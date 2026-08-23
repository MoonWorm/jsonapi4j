package pro.api4.jsonapi4j.plugin.ac.entitlement;

import java.util.List;

/**
 * Marker default for {@code @AccessControlEntitlements(policy = ...)}, meaning "no policy declared".
 * <p>
 * Annotation attributes cannot default to {@code null}, so this stands in for an absent policy the same way
 * {@link pro.api4.jsonapi4j.plugin.ac.ownership.NoOpOwnerIdExtractor} does for an absent owner id extractor.
 * It is never evaluated: the model treats it as "not set" and falls back to the declarative groups.
 */
public class NoOpEntitlementsPolicy implements EntitlementsPolicy {

    @Override
    public boolean isSatisfiedBy(List<String> entitlements) {
        return true;
    }

}
