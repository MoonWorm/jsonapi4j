package pro.api4.jsonapi4j.principal.tier;

/**
 * Registry that maps access tier names to {@link AccessTier} instances.
 * <p>
 * Applications must provide an implementation and register it with the framework so that
 * the Access Control plugin can resolve tier names (e.g. from annotations) to the
 * application-defined tier objects at startup and request time.
 *
 * @see AccessTier
 */
public interface AccessTierRegistry {

    /**
     * Returns the {@link AccessTier} registered under the given name.
     *
     * @param name the tier name to look up
     * @return the corresponding {@link AccessTier}
     * @throws IllegalArgumentException or similar if the name is not registered
     */
    AccessTier getAccessTier(String name);

    /**
     * Returns the {@link AccessTier} registered under the given name, or a framework-defined
     * default tier if the name is not found (e.g. the lowest-privilege tier).
     *
     * @param name the tier name to look up
     * @return the corresponding {@link AccessTier}, or the default tier if not found
     */
    AccessTier getAccessTierOrDefault(String name);

}
