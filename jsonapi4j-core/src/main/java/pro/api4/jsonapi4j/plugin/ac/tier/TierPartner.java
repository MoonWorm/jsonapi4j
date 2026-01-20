
package pro.api4.jsonapi4j.plugin.ac.tier;

public class TierPartner implements AccessTier {

    public static final String PARTNER_ACCESS_TIER = "PARTNER";

    @Override
    public String getName() {
        return PARTNER_ACCESS_TIER;
    }

    @Override
    public int getWeight() {
        return 20;
    }

    @Override
    public String toString() {
        return PARTNER_ACCESS_TIER;
    }

}

