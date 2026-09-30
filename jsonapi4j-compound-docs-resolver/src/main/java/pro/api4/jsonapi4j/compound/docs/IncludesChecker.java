package pro.api4.jsonapi4j.compound.docs;

import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.compound.docs.config.CompoundDocsResolverConfig;
import pro.api4.jsonapi4j.compound.docs.config.UnsupportedIncludeStrategy;
import pro.api4.jsonapi4j.compound.docs.exception.UnsupportedIncludeException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks the {@code include} paths of a request against what the resolver supports - for now, their depth against
 * {@code maxHops}. Cheap and needs no response, so it can run before the primary data is fetched;
 * {@link CompoundDocsResolver} runs it as well. Create it with {@link #from(CompoundDocsResolverConfig)} from the same
 * config as the resolver, so the two agree.
 */
public final class IncludesChecker {

    private final int maxHops;
    private final UnsupportedIncludeStrategy unsupportedIncludes;

    private IncludesChecker(int maxHops, UnsupportedIncludeStrategy unsupportedIncludes) {
        this.maxHops = maxHops;
        this.unsupportedIncludes = unsupportedIncludes;
    }

    public static IncludesChecker from(CompoundDocsResolverConfig config) {
        Validate.notNull(config, "CompoundDocsResolverConfig is not configured");
        return new IncludesChecker(
                config.getMaxHops(),
                Validate.notNull(config.getUnsupportedIncludes(), "unsupportedIncludes is not configured")
        );
    }

    /**
     * @return the paths to resolve. Under {@link UnsupportedIncludeStrategy#IGNORE} a path deeper than supported is cut
     * to the supported depth and reported as a gap
     * @throws UnsupportedIncludeException under {@link UnsupportedIncludeStrategy#FAIL}, naming the first path deeper
     *                                     than supported
     */
    public CheckedIncludes check(CompoundDocsRequest compoundDocsRequest) {
        if (!compoundDocsRequest.isProcessable()) {
            return new CheckedIncludes(List.of(), Set.of());
        }
        List<String> paths = new ArrayList<>();
        Set<IncludedGap> gaps = new HashSet<>();
        for (String include : effectiveIncludes(compoundDocsRequest)) {
            int depth = IncludeTree.depth(include);
            if (depth <= maxHops) {
                paths.add(include);
            } else if (unsupportedIncludes == UnsupportedIncludeStrategy.FAIL) {
                throw new UnsupportedIncludeException(String.format(
                        "Include path '%s' spans %d relationships, more than the supported %d",
                        include,
                        depth,
                        maxHops
                ));
            } else {
                paths.add(IncludeTree.truncate(include, maxHops));
                gaps.add(IncludedGap.forPath(IncompleteReason.UNSUPPORTED_INCLUDE, include));
            }
        }
        return new CheckedIncludes(List.copyOf(paths), Set.copyOf(gaps));
    }

    /**
     * On a relationship endpoint include paths start at the relationship itself, so only those apply.
     */
    private static List<String> effectiveIncludes(CompoundDocsRequest compoundDocsRequest) {
        String relationshipName = compoundDocsRequest.getRelationshipNameFromRequestUri();
        if (relationshipName == null) {
            return compoundDocsRequest.getIncludes();
        }
        return compoundDocsRequest.getIncludes()
                .stream()
                .filter(i -> i.equals(relationshipName) || i.startsWith(relationshipName + "."))
                .toList();
    }

}
