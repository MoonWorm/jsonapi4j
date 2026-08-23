package pro.api4.jsonapi4j.plugin.ac.policy;

import pro.api4.jsonapi4j.plugin.ac.context.AccessControlContext;

/**
 * Marker default for {@code @AccessControlPolicy}, meaning "no policy declared".
 * <p>
 * Annotation attributes cannot default to {@code null}, so this stands in for an absent policy the same way
 * {@link pro.api4.jsonapi4j.plugin.ac.ownership.NoOpOwnerIdExtractor} does for an absent owner id extractor.
 * It is never evaluated: the model treats it as "not set" and declares no policy requirement at all.
 */
public class NoOpAccessPolicy implements AccessPolicy {

    @Override
    public boolean isSatisfiedBy(AccessControlContext context) {
        return true;
    }

}
