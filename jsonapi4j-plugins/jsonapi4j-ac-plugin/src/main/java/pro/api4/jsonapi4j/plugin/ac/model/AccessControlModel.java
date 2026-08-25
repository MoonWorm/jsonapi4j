package pro.api4.jsonapi4j.plugin.ac.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.diagnostics.AccessControlDiagnostics;
import pro.api4.jsonapi4j.util.ReflectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Collections.unmodifiableMap;

@EqualsAndHashCode
@ToString
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
public class AccessControlModel {

    /**
     * Class-level requirements, computed once per class.
     * <p>
     * Annotations cannot change while the JVM runs, so the model derived from a class is valid for that
     * class's lifetime. {@link ClassValue} keys the cache weakly, so an entry is collected together with the
     * class it describes and nothing pins an application classloader.
     */
    private static final ClassValue<Optional<AccessControlModel>> FROM_CLASS_ANNOTATION = new ClassValue<>() {
        @Override
        protected Optional<AccessControlModel> computeValue(Class<?> clazz) {
            AccessControlModel model
                    = fromAnnotation(ReflectionUtils.findAnnotationForClass(clazz, AccessControl.class));
            AccessControlDiagnostics.reportRequirementlessAccessControl(clazz.getName(), model);
            return Optional.ofNullable(model);
        }
    };

    /**
     * Field-level requirements, computed once per class. Cached for the same reason as
     * {@link #FROM_CLASS_ANNOTATION}, and holding an unmodifiable map so the shared value cannot be altered
     * by a caller.
     */
    private static final ClassValue<Map<String, AccessControlModel>> FROM_FIELDS_ANNOTATIONS = new ClassValue<>() {
        @Override
        protected Map<String, AccessControlModel> computeValue(Class<?> clazz) {
            Map<String, AccessControlModel> accessControlModelPerField = new HashMap<>();
            ReflectionUtils.fetchAnnotationForFields(clazz, AccessControl.class)
                    .forEach((fieldName, accessControl) -> {
                        AccessControlModel model = fromAnnotation(accessControl);
                        AccessControlDiagnostics.reportRequirementlessAccessControl(
                                clazz.getName() + "." + fieldName, model);
                        accessControlModelPerField.put(fieldName, model);
                    });
            return unmodifiableMap(accessControlModelPerField);
        }
    };

    private final AccessControlAuthenticatedModel authenticated;
    private final AccessControlEntitlementsModel requiredEntitlements;
    private final AccessControlScopesModel requiredScopes;
    private final AccessControlOwnershipModel requiredOwnership;
    private final AccessControlPolicyModel requiredPolicy;

    /**
     * Tells whether this model asks for anything at all. A model with every requirement absent enforces
     * nothing.
     *
     * @return {@code true} when no requirement of any kind is declared
     */
    public boolean declaresNoRequirements() {
        return authenticated == null
                && requiredEntitlements == null
                && requiredScopes == null
                && requiredOwnership == null
                && requiredPolicy == null;
    }

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

    /**
     * Returns the field-level requirements declared on the given type, keyed by field name.
     * <p>
     * Cached per class like {@link #fromClassAnnotation(Class)}, and for the same reason: the scan walks the
     * type's whole field hierarchy, and it is run at every node of the outbound class-graph descent.
     *
     * @param clazz the type to read
     * @return the requirements per field, unmodifiable and empty when the type declares none
     */
    public static Map<String, AccessControlModel> fromFieldsAnnotations(Class<?> clazz) {
        Validate.notNull(clazz, "type must not be null");
        return FROM_FIELDS_ANNOTATIONS.get(clazz);
    }

    /**
     * Returns the class-level requirements declared on the given type, or {@code null} when it declares none.
     * <p>
     * Cached per class: the reflective annotation scan runs once and every later call is a lookup. The result
     * is wrapped in an {@link Optional} internally so that "no annotation" is cached as explicitly as a
     * requirement is.
     *
     * @param clazz the type to read
     * @return the requirements, or {@code null} when the type declares none
     */
    public static AccessControlModel fromClassAnnotation(Class<?> clazz) {
        Validate.notNull(clazz, "type must not be null");
        return FROM_CLASS_ANNOTATION.get(clazz).orElse(null);
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
                s -> true
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
