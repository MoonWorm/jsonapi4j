package pro.api4.jsonapi4j.plugin.ac.model;

import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlScopes;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Builder
@Getter
public class AccessControlScopesModel {

    public static final AccessControlScopesModel DEFAULT = AccessControlScopesModel.builder().build();

    @Builder.Default
    private Set<String> requiredScopes = new HashSet<>();
    @Builder.Default
    private String requiredScopesExpression = "";

    public static Optional<AccessControlScopesModel> fromAnnotation(AccessControlScopes annotation) {
        return Optional.ofNullable(annotation)
                .filter(ann -> (ann.requiredScopes() != null && ann.requiredScopes().length > 0)
                        || StringUtils.isNotBlank(ann.requiredScopesExpression()))
                .map(a ->
                        AccessControlScopesModel.builder()
                                .requiredScopes(Set.of(a.requiredScopes()))
                                .requiredScopesExpression(a.requiredScopesExpression())
                                .build()
                );
    }

}
