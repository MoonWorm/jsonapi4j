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
import pro.api4.jsonapi4j.plugin.ac.entitlement.EntitlementsPolicy;
import pro.api4.jsonapi4j.plugin.ac.entitlement.NoOpEntitlementsPolicy;
import pro.api4.jsonapi4j.plugin.ac.exception.AccessControlMisconfigurationException;

import java.lang.reflect.InvocationTargetException;
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

    private EntitlementsPolicy policy;

    /**
     * Builds the model for an {@code @AccessControlEntitlements} requirement, rejecting declarations that can
     * never be satisfied or that mean two things at once.
     * <p>
     * A policy, when declared, is instantiated once here rather than per request, so a broken policy class
     * fails at registration instead of on the first request that reaches the annotated element.
     *
     * @param entitlements the annotation to read
     * @return the model, or {@code null} when the annotation declares no requirement at all
     * @throws AccessControlMisconfigurationException if clauses and a policy are both declared, or the policy
     *                                                cannot be instantiated
     */
    static AccessControlEntitlementsModel fromAnnotation(AccessControlEntitlements entitlements) {
        if (entitlements == null) {
            return null;
        }
        boolean policyDeclared = !NoOpEntitlementsPolicy.class.equals(entitlements.policy());
        boolean groupsDeclared = entitlements.value().length > 0;
        if (policyDeclared && groupsDeclared) {
            throw new AccessControlMisconfigurationException(String.format(
                    "@AccessControlEntitlements declares both clauses %s and policy %s. Declare one or the other: "
                            + "a policy replaces the clauses rather than adding to them.",
                    Arrays.toString(entitlements.value()), entitlements.policy().getName()));
        }
        if (!policyDeclared && !groupsDeclared) {
            return null;
        }
        AccessControlEntitlementsModelBuilder builder = AccessControlEntitlementsModel.builder()
                .description(StringUtils.trimToNull(entitlements.description()));
        if (policyDeclared) {
            return builder.policy(instantiate(entitlements.policy())).build();
        }
        return builder
                .groups(Arrays.stream(entitlements.value()).map(EntitlementsGroupModel::fromAnnotation).toList())
                .mode(Mode.from(entitlements.mode()))
                .build();
    }

    private static EntitlementsPolicy instantiate(Class<? extends EntitlementsPolicy> policyType) {
        try {
            return policyType.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                 | NoSuchMethodException e) {
            throw new AccessControlMisconfigurationException(String.format(
                    "Failed to instantiate EntitlementsPolicy %s. It must be a public class with a public "
                            + "no-argument constructor.", policyType.getName()), e);
        }
    }

    /**
     * Decides whether the entitlements a caller holds satisfy this requirement — by delegating to the declared
     * policy, or by combining the clauses with {@link #getMode()}.
     *
     * @param heldEntitlements the entitlements the caller holds, in the order the resolver produced them
     * @return {@code true} when the requirement is satisfied
     */
    public boolean isSatisfiedBy(List<String> heldEntitlements) {
        if (policy != null) {
            return policy.isSatisfiedBy(heldEntitlements);
        }
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
