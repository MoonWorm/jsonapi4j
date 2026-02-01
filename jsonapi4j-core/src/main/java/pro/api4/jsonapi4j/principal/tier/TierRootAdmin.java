
package pro.api4.jsonapi4j.principal.tier;

public class TierRootAdmin implements AccessTier {

    public static final String ROOT_ADMIN_ACCESS_TIER = "ROOT_ADMIN";

    @Override
    public String getName() {
        return ROOT_ADMIN_ACCESS_TIER;
    }

    @Override
    public int getWeight() {
        return 50;
    }

    @Override
    public String toString() {
        return ROOT_ADMIN_ACCESS_TIER;
    }

}

