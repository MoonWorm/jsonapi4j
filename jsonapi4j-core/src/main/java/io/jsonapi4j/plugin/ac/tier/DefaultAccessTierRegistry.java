package io.jsonapi4j.plugin.ac.tier;

import org.apache.commons.lang3.Validate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultAccessTierRegistry implements AccessTierRegistry {

    private final Map<String, AccessTier> registeredAccessTiers = new HashMap<>();
    private final AccessTier defaultAccessTier;

    public DefaultAccessTierRegistry() {
        this(DefaultAccessTierRegistry.getDefaultAccessTiers(), new TierPublic());
    }

    public DefaultAccessTierRegistry(List<AccessTier> accessTiers,
                                     AccessTier defaultAccessTier) {
        Validate.notNull(accessTiers, "Access tiers must not be null");
        Validate.notNull(defaultAccessTier, "Default access tier must not be null");
        accessTiers.forEach(this::registerAccessTier);
        this.defaultAccessTier = defaultAccessTier;
    }

    private static List<AccessTier> getDefaultAccessTiers() {
        return List.of(
                new TierNoAccess(),
                new TierPublic(),
                new TierPartner(),
                new TierAdmin(),
                new TierRootAdmin()
        );
    }

    public void registerAccessTier(AccessTier accessTier) {
        registeredAccessTiers.put(accessTier.getName(), accessTier);
    }

    @Override
    public AccessTier getAccessTier(String name) {
        return registeredAccessTiers.get(name);
    }

    @Override
    public AccessTier getAccessTierOrDefault(String name) {
        AccessTier accessTier = getAccessTier(name);
        if (accessTier == null) {
            return defaultAccessTier;
        }
        return accessTier;
    }

}
