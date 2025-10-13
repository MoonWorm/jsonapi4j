package io.jsonapi4j.plugin.ac.scope;

import org.apache.commons.lang3.Validate;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.List;
import java.util.Set;

public final class ScopesUtils {

    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();

    private ScopesUtils() {

    }

    public static boolean matches(Set<String> actualScopes, String scopesExpression) {
        StandardEvaluationContext context = new StandardEvaluationContext(new ScopesContext(actualScopes));
        Expression expression = SPEL_PARSER.parseExpression(scopesExpression);
        return Boolean.TRUE.equals(expression.getValue(context, Boolean.class));
    }

    public static String toScopesExpression(Set<String> requiredScopes) {
        Validate.notNull(requiredScopes);
        List<String> requiredScopesExrItems = requiredScopes
                .stream()
                .sorted()
                .map(s -> "hasScope('" + s + "')")
                .toList();
        return String.join(" AND ", requiredScopesExrItems);
    }

    private static class ScopesContext {

        private final Set<String> actualScopes;

        public ScopesContext(Set<String> actualScopes) {
            this.actualScopes = actualScopes;
        }

        public boolean hasScope(String scope) {
            return actualScopes.contains(scope);
        }

    }
}


