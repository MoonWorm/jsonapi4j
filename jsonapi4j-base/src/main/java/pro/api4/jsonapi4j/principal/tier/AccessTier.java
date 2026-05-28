package pro.api4.jsonapi4j.principal.tier;


/**
 * Represents a named access tier used for coarse-grained authorization.
 * <p>
 * Access tiers form an ordered hierarchy based on their {@link #getWeight()} value —
 * a higher weight indicates a higher privilege level. The Access Control plugin uses this
 * ordering when evaluating whether an authenticated client has sufficient access for a given operation.
 * <p>
 * Applications define their own set of tiers (e.g. as an enum implementing this interface)
 * and register them via {@link AccessTierRegistry}.
 *
 * @see AccessTierRegistry
 * @see pro.api4.jsonapi4j.principal.Principal#authenticatedClientAccessTier()
 */
public interface AccessTier extends Comparable<AccessTier> {

    /**
     * Returns the unique name of this access tier (e.g. {@code "PUBLIC"}, {@code "INTERNAL"}).
     *
     * @return tier name, never {@code null}
     */
    String getName();

    /**
     * Returns the numeric weight of this tier, used for ordering and comparison.
     * Higher values indicate higher privilege levels.
     *
     * @return tier weight
     */
    int getWeight();

    /**
     * Compares this tier to another by weight (ascending order).
     *
     * @param o the other access tier
     * @return negative, zero, or positive as this tier's weight is less than, equal to, or greater than {@code o}'s
     */
    @Override
    default int compareTo(AccessTier o) {
        return Integer.compare(getWeight(), o.getWeight());
    }

}

