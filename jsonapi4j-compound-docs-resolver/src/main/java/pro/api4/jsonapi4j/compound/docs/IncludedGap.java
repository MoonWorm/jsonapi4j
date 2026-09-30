package pro.api4.jsonapi4j.compound.docs;

import java.util.Comparator;

/**
 * Resources of {@code type} missing from {@code included}, and why. Which ones exactly follows from the document:
 * those linked in {@code relationships} with no matching resource object in {@code included}.
 */
public record IncludedGap(IncompleteReason reason, String type) implements Comparable<IncludedGap> {

    private static final Comparator<IncludedGap> ORDER = Comparator.comparing(IncludedGap::type)
            .thenComparing(IncludedGap::reason);

    @Override
    public int compareTo(IncludedGap other) {
        return ORDER.compare(this, other);
    }

}
