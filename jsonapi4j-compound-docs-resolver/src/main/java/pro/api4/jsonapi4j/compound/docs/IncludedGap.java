package pro.api4.jsonapi4j.compound.docs;

import java.util.Comparator;

/**
 * A part of the requested {@code included} member that is missing from the document, and why. Names either the
 * {@code type} whose resources could not be fetched, or the include {@code path} that was resolved no further.
 * Which resources exactly are missing follows from the document: those linked in {@code relationships} with no
 * matching resource object in {@code included}.
 *
 * @param type the resource type whose resources are missing, or {@code null} for a path
 * @param path the include path resolved no further, or {@code null} for a type
 */
public record IncludedGap(IncompleteReason reason, String type, String path) implements Comparable<IncludedGap> {

    private static final Comparator<IncludedGap> ORDER = Comparator.comparing(IncludedGap::reason)
            .thenComparing(IncludedGap::type, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(IncludedGap::path, Comparator.nullsLast(Comparator.naturalOrder()));

    public static IncludedGap forType(IncompleteReason reason, String type) {
        return new IncludedGap(reason, type, null);
    }

    public static IncludedGap forPath(IncompleteReason reason, String path) {
        return new IncludedGap(reason, null, path);
    }

    @Override
    public int compareTo(IncludedGap other) {
        return ORDER.compare(this, other);
    }

}
