package pro.api4.jsonapi4j.compound.docs.exception;

import lombok.Getter;

import java.util.Set;

/**
 * A downstream service answered a fetch with {@code 400 UNSUPPORTED_INCLUDE}: resources of {@code resourceType} have
 * no relationships named {@code relationshipNames}, though the request asked to include them. The resolver maps them
 * back to the include paths of the original request, so it never leaves the resolver.
 */
@Getter
public class RejectedIncludesException extends RuntimeException {

    private final String resourceType;
    private final Set<String> relationshipNames;

    public RejectedIncludesException(String resourceType, Set<String> relationshipNames) {
        super(String.format("Resource type '%s' rejected includes %s", resourceType, relationshipNames));
        this.resourceType = resourceType;
        this.relationshipNames = Set.copyOf(relationshipNames);
    }

}
