package pro.api4.jsonapi4j.plugin.ac.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.plugin.ac.annotation.EntitlementsGroup;
import pro.api4.jsonapi4j.plugin.ac.diagnostics.AccessControlDiagnostics;
import pro.api4.jsonapi4j.plugin.ac.exception.AccessControlMisconfigurationException;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@EqualsAndHashCode
@ToString
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
public class EntitlementsGroupModel {

    private final Set<String> entitlements;

    @Builder.Default
    private final Mode mode = Mode.ANY_OF;

    /**
     * Builds the model for a single {@code @EntitlementsGroup} clause, rejecting clauses that can never be
     * satisfied. A clause naming no entitlement, or a blank one, is a misconfiguration rather than an absent
     * requirement — absence is expressed by leaving the clause out altogether.
     *
     * @param group the clause to read
     * @return the model, never {@code null}
     * @throws AccessControlMisconfigurationException if the clause is empty or names a blank entitlement
     */
    static EntitlementsGroupModel fromAnnotation(EntitlementsGroup group) {
        if (group == null) {
            throw AccessControlDiagnostics.missingEntitlementsGroup();
        }
        Set<String> entitlements = new LinkedHashSet<>(Arrays.asList(group.value()));
        if (entitlements.isEmpty() || entitlements.stream().anyMatch(StringUtils::isBlank)) {
            throw AccessControlDiagnostics.emptyEntitlementsGroup(group.value());
        }
        return EntitlementsGroupModel.builder()
                .entitlements(entitlements)
                .mode(Mode.from(group.mode()))
                .build();
    }

    /**
     * Decides whether the entitlements a caller holds satisfy this clause.
     *
     * @param heldEntitlements the entitlements the caller holds
     * @return {@code true} when the clause is satisfied
     */
    public boolean isSatisfiedBy(Set<String> heldEntitlements) {
        return switch (mode) {
            case ALL_OF -> heldEntitlements.containsAll(entitlements);
            case ANY_OF -> entitlements.stream().anyMatch(heldEntitlements::contains);
            case NONE_OF -> entitlements.stream().noneMatch(heldEntitlements::contains);
        };
    }

    /**
     * Copies the given names defensively, preserving declaration order.
     * <p>
     * The model is cached and shared for the lifetime of the class it describes, so handing out — or holding
     * on to — a caller's mutable set would let one caller alter the requirement for every later request.
     */
    public static class EntitlementsGroupModelBuilder {
        public EntitlementsGroupModelBuilder entitlements(Set<String> entitlements) {
            this.entitlements = entitlements == null
                    ? null
                    : Collections.unmodifiableSet(new LinkedHashSet<>(entitlements));
            return this;
        }
    }

    public enum Mode {

        ALL_OF,
        ANY_OF,
        NONE_OF;

        static Mode from(EntitlementsGroup.Mode mode) {
            return switch (mode) {
                case ALL_OF -> ALL_OF;
                case ANY_OF -> ANY_OF;
                case NONE_OF -> NONE_OF;
            };
        }
    }

}
