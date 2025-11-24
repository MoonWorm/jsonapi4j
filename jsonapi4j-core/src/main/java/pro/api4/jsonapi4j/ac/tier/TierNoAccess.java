package pro.api4.jsonapi4j.ac.tier;

public class TierNoAccess implements AccessTier {

    public static final String NO_ACCESS_TIER = "NO_ACCESS";

    @Override
    public String getName() {
        return NO_ACCESS_TIER;
    }

    @Override
    public int getWeight() {
        return 0;
    }

    @Override
    public String toString() {
        return NO_ACCESS_TIER;
    }

}
