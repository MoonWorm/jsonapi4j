package pro.api4.jsonapi4j.compound.docs;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The requested {@code include} paths as a tree. A path is a dot-separated chain of relationship names starting at
 * the primary data, which itself is {@link #ROOT}.
 */
final class IncludeTree {

    static final String ROOT = "";

    private final Map<String, Set<String>> childrenByPath;

    private IncludeTree(Map<String, Set<String>> childrenByPath) {
        this.childrenByPath = childrenByPath;
    }

    static IncludeTree of(Collection<String> includes) {
        Map<String, Set<String>> childrenByPath = new HashMap<>();
        if (includes != null) {
            for (String include : includes) {
                String path = ROOT;
                for (String relationshipName : include.split("\\.")) {
                    childrenByPath.computeIfAbsent(path, p -> new LinkedHashSet<>()).add(relationshipName);
                    path = childPath(path, relationshipName);
                }
            }
        }
        return new IncludeTree(childrenByPath);
    }

    static String childPath(String path, String relationshipName) {
        return ROOT.equals(path) ? relationshipName : path + "." + relationshipName;
    }

    /**
     * @return relationship names requested right after {@code path}
     */
    Set<String> children(String path) {
        return childrenByPath.getOrDefault(path, Collections.emptySet());
    }

}
