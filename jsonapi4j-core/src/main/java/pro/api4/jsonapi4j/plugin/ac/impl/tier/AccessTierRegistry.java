package pro.api4.jsonapi4j.plugin.ac.impl.tier;

public interface AccessTierRegistry {

    AccessTier getAccessTier(String name);

    AccessTier getAccessTierOrDefault(String name);

}
