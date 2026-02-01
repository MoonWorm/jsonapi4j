package pro.api4.jsonapi4j.principal.tier;

public interface AccessTierRegistry {

    AccessTier getAccessTier(String name);

    AccessTier getAccessTierOrDefault(String name);

}
