package pro.api4.jsonapi4j.ac.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import pro.api4.jsonapi4j.ac.annotation.AccessControlScopes;

import java.util.Collections;
import java.util.Set;

@EqualsAndHashCode
@ToString
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
public class AccessControlScopesModel {

    private Set<String> requiredScopes;
    private String requiredScopesExpression;

    static AccessControlScopesModel fromAnnotation(AccessControlScopes annotation) {
        if (annotation == null) {
            return null;
        }
        return AccessControlScopesModel.builder()
                .requiredScopes(annotation.requiredScopes() == null ? Collections.emptySet() : Set.of(annotation.requiredScopes()))
                .requiredScopesExpression(annotation.requiredScopesExpression())
                .build();
    }

}
