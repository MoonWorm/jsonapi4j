package pro.api4.jsonapi4j.plugin.ac.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlScopes;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@EqualsAndHashCode
@ToString
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
public class AccessControlScopesModel {

    @Builder.Default
    private List<ScopesGroupModel> groups = List.of();

    @Builder.Default
    private Mode mode = Mode.ALL_OF;

    private String description;

    /**
     * Builds the model for an {@code @AccessControlScopes} requirement.
     *
     * @param annotation the annotation to read
     * @return the model, or {@code null} when the annotation declares no clauses at all
     */
    static AccessControlScopesModel fromAnnotation(AccessControlScopes annotation) {
        if (annotation == null || annotation.value().length == 0) {
            return null;
        }
        return AccessControlScopesModel.builder()
                .description(StringUtils.trimToNull(annotation.description()))
                .groups(Arrays.stream(annotation.value()).map(ScopesGroupModel::fromAnnotation).toList())
                .mode(Mode.from(annotation.mode()))
                .build();
    }

    /**
     * Decides whether the scopes granted to a caller satisfy this requirement, by combining the clauses with
     * {@link #getMode()}.
     *
     * @param grantedScopes the scopes the caller was granted, never {@code null}
     * @return {@code true} when the requirement is satisfied
     */
    public boolean isSatisfiedBy(Set<String> grantedScopes) {
        if (CollectionUtils.isEmpty(groups)) {
            return true;
        }
        return switch (mode) {
            case ALL_OF -> groups.stream().allMatch(g -> g.isSatisfiedBy(grantedScopes));
            case ANY_OF -> groups.stream().anyMatch(g -> g.isSatisfiedBy(grantedScopes));
            case NONE_OF -> groups.stream().noneMatch(g -> g.isSatisfiedBy(grantedScopes));
        };
    }

    public enum Mode {

        ALL_OF,
        ANY_OF,
        NONE_OF;

        static Mode from(AccessControlScopes.Mode mode) {
            return switch (mode) {
                case ALL_OF -> ALL_OF;
                case ANY_OF -> ANY_OF;
                case NONE_OF -> NONE_OF;
            };
        }
    }

}
