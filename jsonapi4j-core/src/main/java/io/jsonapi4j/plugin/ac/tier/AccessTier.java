package io.jsonapi4j.plugin.ac.tier;


public interface AccessTier extends Comparable<AccessTier> {

    String getName();

    int getWeight();

    @Override
    default int compareTo(AccessTier o) {
        return Integer.compare(getWeight(), o.getWeight());
    }

}

