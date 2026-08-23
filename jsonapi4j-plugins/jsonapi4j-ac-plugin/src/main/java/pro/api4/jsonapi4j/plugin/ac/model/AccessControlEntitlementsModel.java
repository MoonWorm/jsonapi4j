package pro.api4.jsonapi4j.plugin.ac.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlEntitlements;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EqualsAndHashCode
@ToString
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
public class AccessControlEntitlementsModel {

    @Builder.Default
    private List<EntitlementsGroupModel> groups = List.of();

    @Builder.Default
    private Mode mode = Mode.ALL_OF;

    private String description;

    /**
     * Builds the model for an {@code @AccessControlEntitlements} requirement.
     *
     * @param entitlements the annotation to read
     * @return the model, or {@code null} when the annotation declares no clauses at all
     */
    static AccessControlEntitlementsModel fromAnnotation(AccessControlEntitlements entitlements) {
        if (entitlements == null || entitlements.value().length == 0) {
            return null;
        }
        return AccessControlEntitlementsModel.builder()
                .description(StringUtils.trimToNull(entitlements.description()))
                .groups(Arrays.stream(entitlements.value()).map(EntitlementsGroupModel::fromAnnotation).toList())
                .mode(Mode.from(entitlements.mode()))
                .build();
    }

    /**
     * Decides whether the entitlements a caller holds satisfy this requirement, by combining the clauses with
     * {@link #getMode()}.
     *
     * @param heldEntitlements the entitlements the caller holds, in the order the resolver produced them
     * @return {@code true} when the requirement is satisfied
     */
    public boolean isSatisfiedBy(List<String> heldEntitlements) {
        if (CollectionUtils.isEmpty(groups)) {
            return true;
        }
        Set<String> held = new HashSet<>(heldEntitlements);
        return switch (mode) {
            case ALL_OF -> groups.stream().allMatch(g -> g.isSatisfiedBy(held));
            case ANY_OF -> groups.stream().anyMatch(g -> g.isSatisfiedBy(held));
            case NONE_OF -> groups.stream().noneMatch(g -> g.isSatisfiedBy(held));
        };
    }

    public enum Mode {

        ALL_OF,
        ANY_OF,
        NONE_OF;

        static Mode from(AccessControlEntitlements.Mode mode) {
            return switch (mode) {
                case ALL_OF -> ALL_OF;
                case ANY_OF -> ANY_OF;
                case NONE_OF -> NONE_OF;
            };
        }
    }

}
