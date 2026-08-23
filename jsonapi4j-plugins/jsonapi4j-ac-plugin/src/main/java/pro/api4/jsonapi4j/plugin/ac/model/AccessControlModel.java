package pro.api4.jsonapi4j.plugin.ac.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.util.ReflectionUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Collections.unmodifiableMap;

@EqualsAndHashCode
@ToString
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
public class AccessControlModel {

    private AccessControlAuthenticatedModel authenticated;
    private AccessControlEntitlementsModel requiredEntitlements;
    private AccessControlScopesModel requiredScopes;
    private AccessControlOwnershipModel requiredOwnership;
    private AccessControlPolicyModel requiredPolicy;

    public static AccessControlModel fromAnnotation(AccessControl annotation) {
        if (annotation == null) {
            return null;
        }
        AccessControlAuthenticatedModel authenticatedModel = AccessControlAuthenticatedModel.fromValue(annotation.authenticated());
        AccessControlEntitlementsModel entitlementsModel = AccessControlEntitlementsModel.fromAnnotation(annotation.entitlements());
        AccessControlScopesModel scopesModel = AccessControlScopesModel.fromAnnotation(annotation.scopes());
        AccessControlOwnershipModel ownershipModel = AccessControlOwnershipModel.fromAnnotation(annotation.ownership());
        AccessControlPolicyModel policyModel = AccessControlPolicyModel.fromAnnotation(annotation.policy());
        return builder()
                .authenticated(authenticatedModel)
                .requiredEntitlements(entitlementsModel)
                .requiredScopes(scopesModel)
                .requiredOwnership(ownershipModel)
                .requiredPolicy(policyModel)
                .build();
    }

    public static Map<String, AccessControlModel> fromFieldsAnnotations(Class<?> clazz) {
        Validate.notNull(clazz, "type must not be null");
        Map<String, AccessControl> accessControlAnnotationPerField
                = ReflectionUtils.fetchAnnotationForFields(clazz, AccessControl.class);
        Map<String, AccessControlModel> accessControlModelPerField = new HashMap<>();
        accessControlAnnotationPerField.forEach((fieldName, accessControl) -> {
            AccessControlAuthenticatedModel authenticatedModel = AccessControlAuthenticatedModel.fromValue(accessControl.authenticated());
            AccessControlEntitlementsModel entitlementsModel = AccessControlEntitlementsModel.fromAnnotation(accessControl.entitlements());
            AccessControlScopesModel scopesModel = AccessControlScopesModel.fromAnnotation(accessControl.scopes());
            AccessControlOwnershipModel ownershipModel = AccessControlOwnershipModel.fromAnnotation(accessControl.ownership());
            AccessControlPolicyModel policyModel = AccessControlPolicyModel.fromAnnotation(accessControl.policy());
            accessControlModelPerField.put(fieldName, AccessControlModel.builder()
                    .authenticated(authenticatedModel)
                    .requiredEntitlements(entitlementsModel)
                    .requiredScopes(scopesModel)
                    .requiredOwnership(ownershipModel)
                    .requiredPolicy(policyModel)
                    .build());
        });
        return unmodifiableMap(accessControlModelPerField);
    }

    public static AccessControlModel fromClassAnnotation(Class<?> clazz) {
        Validate.notNull(clazz, "type must not be null");
        AccessControl annotation = ReflectionUtils.findAnnotationForClass(clazz, AccessControl.class);
        return fromAnnotation(annotation);
    }

    public static AccessControlModel merge(AccessControlModel lowerPrecedence,
                                           AccessControlModel higherPrecedence) {
        if (lowerPrecedence == null && higherPrecedence == null) {
            return null;
        }
        AccessControlModel.AccessControlModelBuilder resultBuilder = builder();
        if (higherPrecedence != null
                && higherPrecedence.getAuthenticated() != null
                && higherPrecedence.getAuthenticated().getAuthenticated() != null) {
            resultBuilder.authenticated(
                    AccessControlAuthenticatedModel.fromValue(higherPrecedence.getAuthenticated().getAuthenticated())
            );
        } else if (lowerPrecedence != null
                && lowerPrecedence.getAuthenticated() != null
                && lowerPrecedence.getAuthenticated().getAuthenticated() != null) {
            resultBuilder.authenticated(
                    AccessControlAuthenticatedModel.fromValue(lowerPrecedence.getAuthenticated().getAuthenticated())
            );
        }
        AccessControlEntitlementsModel entitlements = winning(
                higherPrecedence, lowerPrecedence,
                AccessControlModel::getRequiredEntitlements,
                e -> true
        );
        if (entitlements != null) {
            resultBuilder.requiredEntitlements(entitlements);
        }
        AccessControlScopesModel scopes = winning(
                higherPrecedence, lowerPrecedence,
                AccessControlModel::getRequiredScopes,
                s -> CollectionUtils.isNotEmpty(s.getRequiredScopes())
                        || StringUtils.isNotBlank(s.getRequiredScopesExpression())
        );
        if (scopes != null) {
            resultBuilder.requiredScopes(scopes);
        }
        AccessControlOwnershipModel ownership = winning(
                higherPrecedence, lowerPrecedence,
                AccessControlModel::getRequiredOwnership,
                o -> StringUtils.isNotBlank(o.getOwnerIdFieldPath()) || o.getOwnerIdExtractor() != null
        );
        if (ownership != null) {
            resultBuilder.requiredOwnership(ownership);
        }
        AccessControlPolicyModel policy = winning(
                higherPrecedence, lowerPrecedence,
                AccessControlModel::getRequiredPolicy,
                p -> true
        );
        if (policy != null) {
            resultBuilder.requiredPolicy(policy);
        }
        return resultBuilder.build();
    }

    /**
     * Picks one requirement out of the two models being merged: the higher-precedence one when it carries a
     * usable value, the lower-precedence one otherwise. Requirements are all-or-nothing — a model that
     * defines one never has its value blended with the other's.
     * <p>
     * The winning requirement is shared rather than copied. Requirement models expose no mutators and are
     * built only from immutable values, so a merged model cannot observe a change made through the model it
     * was merged from.
     *
     * @param higherPrecedence the model whose requirements win, may be {@code null}
     * @param lowerPrecedence  the model consulted when the higher one defines nothing, may be {@code null}
     * @param requirement      reads the requirement being merged out of a model
     * @param isDefined        tells whether a non-{@code null} requirement actually carries a value
     * @param <REQUIREMENT>    the requirement type being merged
     * @return the winning requirement, or {@code null} if neither model defines one
     */
    private static <REQUIREMENT> REQUIREMENT winning(AccessControlModel higherPrecedence,
                                                     AccessControlModel lowerPrecedence,
                                                     Function<AccessControlModel, REQUIREMENT> requirement,
                                                     Predicate<REQUIREMENT> isDefined) {
        REQUIREMENT higher = higherPrecedence == null ? null : requirement.apply(higherPrecedence);
        if (higher != null && isDefined.test(higher)) {
            return higher;
        }
        REQUIREMENT lower = lowerPrecedence == null ? null : requirement.apply(lowerPrecedence);
        return lower != null && isDefined.test(lower) ? lower : null;
    }

}
