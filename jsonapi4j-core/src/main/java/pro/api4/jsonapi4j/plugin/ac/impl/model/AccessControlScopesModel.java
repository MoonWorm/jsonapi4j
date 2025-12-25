package pro.api4.jsonapi4j.plugin.ac.impl.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.plugin.ac.impl.annotation.AccessControlScopes;

import java.util.Arrays;
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
        if (annotation == null
                || (!isRequiredScopesExpressionDefined(annotation.requiredScopesExpression()) && !isRequiredScopesDefined(annotation.requiredScopes()))) {
            return null;
        }
        return AccessControlScopesModel.builder()
                .requiredScopes(isRequiredScopesDefined(annotation.requiredScopes()) ? Set.of(annotation.requiredScopes()) : Collections.emptySet())
                .requiredScopesExpression(isRequiredScopesExpressionDefined(annotation.requiredScopesExpression()) ? annotation.requiredScopesExpression() : null)
                .build();
    }

    private static boolean isRequiredScopesExpressionDefined(String requiredScopesExpression) {
        return StringUtils.isNotBlank(requiredScopesExpression) && !AccessControlScopes.NOT_SET.equals(requiredScopesExpression);
    }

    private static boolean isRequiredScopesDefined(String[] requiredScopes) {
        return requiredScopes != null && !Arrays.equals(new String[]{AccessControlScopes.NOT_SET}, requiredScopes);
    }

}
