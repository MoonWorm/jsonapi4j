package pro.api4.jsonapi4j.plugin.oas.customizer.util;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties.OAuth2;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties.OAuth2GrantFlow;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties.OAuth2Scope;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The OAuth2 grant flows {@code jsonapi4j.oas.oauth2} configures, and the scopes each one declares.
 * <p>
 * A security requirement names a scheme together with the scopes it must be granted, and OpenAPI reads those scopes
 * against <em>that scheme's</em> flow. A scope the named scheme does not declare is a reference to nothing: Swagger
 * UI's authorize dialog cannot offer it, and a linter rejects the document. Scopes are therefore matched per scheme
 * rather than against the union of every flow's - two flows exist precisely because they are granted different
 * things, and a scope declared under one says nothing about the other.
 */
public final class OasSecuritySchemes {

    /**
     * The accessors of every grant flow this plugin supports, in the order the document lists them.
     */
    private static final List<Function<OAuth2, OAuth2GrantFlow>> GRANT_FLOWS = List.of(
            OAuth2::clientCredentials,
            OAuth2::authorizationCodeWithPkce
    );

    private OasSecuritySchemes() {
    }

    /**
     * @return the configured grant flow, empty when the section is absent or the flow has no name - a nameless flow
     * is not a scheme anything can reference
     */
    public static Optional<OAuth2GrantFlow> grantFlow(OasProperties oasProperties,
                                                      Function<OAuth2, OAuth2GrantFlow> grantFlowAccessor) {
        if (oasProperties == null || oasProperties.oauth2() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(grantFlowAccessor.apply(oasProperties.oauth2()))
                .filter(grantFlow -> StringUtils.isNotBlank(grantFlow.name()));
    }

    /**
     * @return the name the document publishes this grant flow under, or {@code null} when it is not configured
     */
    public static String schemeName(OasProperties oasProperties,
                                    Function<OAuth2, OAuth2GrantFlow> grantFlowAccessor) {
        return grantFlow(oasProperties, grantFlowAccessor).map(OAuth2GrantFlow::name).orElse(null);
    }

    /**
     * @return every configured scheme name, in document order
     */
    public static List<String> schemeNames(OasProperties oasProperties) {
        return GRANT_FLOWS.stream()
                .map(accessor -> grantFlow(oasProperties, accessor))
                .flatMap(Optional::stream)
                .map(OAuth2GrantFlow::name)
                .toList();
    }

    /**
     * @return scheme name to the scopes that scheme declares; a configured flow that enumerates none maps to an
     * empty set, which is what makes it unable to carry a scoped requirement
     */
    public static Map<String, Set<String>> declaredScopesByScheme(OasProperties oasProperties) {
        Map<String, Set<String>> byScheme = new LinkedHashMap<>();
        GRANT_FLOWS.forEach(accessor -> grantFlow(oasProperties, accessor).ifPresent(grantFlow -> byScheme.put(
                grantFlow.name(),
                CollectionUtils.emptyIfNull(grantFlow.scopes()).stream()
                        .map(OAuth2Scope::name)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toCollection(LinkedHashSet::new))
        )));
        return byScheme;
    }

    /**
     * @return every scope any configured flow declares - what tells a typo apart from a scope hung off the wrong flow
     */
    public static Set<String> declaredScopes(OasProperties oasProperties) {
        return declaredScopesByScheme(oasProperties).values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Whether a scheme can carry a requirement: it has to declare every scope in it, since a requirement asks for all
     * of them at once. A scheme enumerating no scopes therefore carries only an unscoped requirement.
     *
     * @param declaredScopesByScheme as returned by {@link #declaredScopesByScheme(OasProperties)}
     */
    public static boolean carries(Map<String, Set<String>> declaredScopesByScheme,
                                  String schemeName,
                                  Collection<String> scopes) {
        return declaredScopesByScheme.getOrDefault(schemeName, Set.of()).containsAll(scopes);
    }

    /**
     * Says which scheme is short of which scopes, so a diagnostic names the flow to fix rather than only the scope
     * that went unpublished.
     */
    public static String describeMissingScopes(Map<String, Set<String>> declaredScopesByScheme,
                                               Collection<String> schemeNames,
                                               Collection<String> scopes) {
        return schemeNames.stream()
                .map(schemeName -> {
                    Set<String> missing = new LinkedHashSet<>(scopes);
                    missing.removeAll(declaredScopesByScheme.getOrDefault(schemeName, Set.of()));
                    return missing.isEmpty()
                            ? null
                            : String.format("'%s' does not declare %s", schemeName, String.join(", ", missing));
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining("; "));
    }

}
