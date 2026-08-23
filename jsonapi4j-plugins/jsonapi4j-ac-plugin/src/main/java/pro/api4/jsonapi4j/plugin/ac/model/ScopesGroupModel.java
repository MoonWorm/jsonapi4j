package pro.api4.jsonapi4j.plugin.ac.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.plugin.ac.annotation.ScopesGroup;
import pro.api4.jsonapi4j.plugin.ac.exception.AccessControlMisconfigurationException;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@EqualsAndHashCode
@ToString
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
public class ScopesGroupModel {

    private Set<String> scopes;

    @Builder.Default
    private Mode mode = Mode.ALL_OF;

    /**
     * Builds the model for a single {@code @ScopesGroup} clause, rejecting clauses that can never be
     * satisfied. A clause naming no scope, or a blank one, is a misconfiguration rather than an absent
     * requirement — absence is expressed by leaving the clause out altogether.
     *
     * @param group the clause to read
     * @return the model, never {@code null}
     * @throws AccessControlMisconfigurationException if the clause is empty or names a blank scope
     */
    static ScopesGroupModel fromAnnotation(ScopesGroup group) {
        if (group == null) {
            throw new AccessControlMisconfigurationException(
                    "@ScopesGroup must not be null. Omit it from @AccessControlScopes to declare no requirement.");
        }
        Set<String> scopes = new LinkedHashSet<>(Arrays.asList(group.value()));
        if (scopes.isEmpty() || scopes.stream().anyMatch(StringUtils::isBlank)) {
            throw new AccessControlMisconfigurationException(String.format(
                    "@ScopesGroup must name at least one non-blank scope, but was %s. "
                            + "Omit it from @AccessControlScopes to declare no requirement.",
                    Arrays.toString(group.value())));
        }
        return ScopesGroupModel.builder()
                .scopes(scopes)
                .mode(Mode.from(group.mode()))
                .build();
    }

    /**
     * Decides whether the scopes granted to a caller satisfy this clause.
     *
     * @param grantedScopes the scopes the caller was granted
     * @return {@code true} when the clause is satisfied
     */
    public boolean isSatisfiedBy(Set<String> grantedScopes) {
        return switch (mode) {
            case ALL_OF -> grantedScopes.containsAll(scopes);
            case ANY_OF -> scopes.stream().anyMatch(grantedScopes::contains);
            case NONE_OF -> scopes.stream().noneMatch(grantedScopes::contains);
        };
    }

    public enum Mode {

        ALL_OF,
        ANY_OF,
        NONE_OF;

        static Mode from(ScopesGroup.Mode mode) {
            return switch (mode) {
                case ALL_OF -> ALL_OF;
                case ANY_OF -> ANY_OF;
                case NONE_OF -> NONE_OF;
            };
        }
    }

}
