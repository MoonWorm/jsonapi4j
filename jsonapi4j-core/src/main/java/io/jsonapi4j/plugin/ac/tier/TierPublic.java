package io.jsonapi4j.plugin.ac.tier;

public class TierPublic implements AccessTier {

    public static final String PUBLIC_TIER = "PUBLIC";

    @Override
    public String getName() {
        return PUBLIC_TIER;
    }

    @Override
    public int getWeight() {
        return 10;
    }

    @Override
    public String toString() {
        return PUBLIC_TIER;
    }

}

