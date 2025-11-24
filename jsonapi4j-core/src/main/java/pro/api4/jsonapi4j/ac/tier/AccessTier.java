package pro.api4.jsonapi4j.ac.tier;


public interface AccessTier extends Comparable<AccessTier> {

    String getName();

    int getWeight();

    @Override
    default int compareTo(AccessTier o) {
        return Integer.compare(getWeight(), o.getWeight());
    }

}

