package io.jsonapi4j.plugin.ac.tier;

public interface AccessTierRegistry {

    AccessTier getAccessTier(String name);

    AccessTier getAccessTierOrDefault(String name);

}
