package pro.api4.jsonapi4j.compound.docs;

import java.util.List;
import java.util.Set;

/**
 * The include paths of a request as they will be resolved, after {@link IncludesChecker#check}.
 *
 * @param paths the paths to resolve - each unsupported one cut to the supported depth
 * @param gaps  an {@link IncompleteReason#UNSUPPORTED_INCLUDE} gap for each path that was cut
 */
public record CheckedIncludes(List<String> paths, Set<IncludedGap> gaps) {
}
