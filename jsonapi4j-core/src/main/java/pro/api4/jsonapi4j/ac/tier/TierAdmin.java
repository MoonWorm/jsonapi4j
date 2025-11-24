
package pro.api4.jsonapi4j.ac.tier;

public class TierAdmin implements AccessTier {

    public static final String ADMIN_ACCESS_TIER = "ADMIN";

    @Override
    public String getName() {
        return ADMIN_ACCESS_TIER;
    }

    @Override
    public int getWeight() {
        return 30;
    }

    @Override
    public String toString() {
        return ADMIN_ACCESS_TIER;
    }

}

