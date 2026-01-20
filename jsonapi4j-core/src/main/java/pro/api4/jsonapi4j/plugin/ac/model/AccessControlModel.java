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
import pro.api4.jsonapi4j.plugin.utils.ReflectionUtils;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

@EqualsAndHashCode
@ToString
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
public class AccessControlModel {

    private AccessControlAuthenticatedModel authenticated;
    private AccessControlAccessTierModel requiredAccessTier;
    private AccessControlScopesModel requiredScopes;
    private AccessControlOwnershipModel requiredOwnership;

    public static AccessControlModel fromAnnotation(AccessControl annotation) {
        if (annotation == null) {
            return null;
        }
        AccessControlAuthenticatedModel authenticatedModel = AccessControlAuthenticatedModel.fromValue(annotation.authenticated());
        AccessControlAccessTierModel accessTierModel = AccessControlAccessTierModel.fromAnnotation(annotation.tier());
        AccessControlScopesModel scopesModel = AccessControlScopesModel.fromAnnotation(annotation.scopes());
        AccessControlOwnershipModel ownershipModel = AccessControlOwnershipModel.fromAnnotation(annotation.ownership());
        if (authenticatedModel == null && accessTierModel == null && scopesModel == null && ownershipModel == null) {
            return null;
        }
        return builder()
                .authenticated(authenticatedModel)
                .requiredAccessTier(accessTierModel)
                .requiredScopes(scopesModel)
                .requiredOwnership(ownershipModel)
                .build();
    }

    public static Map<String, AccessControlModel> fromFieldsAnnotations(Class<?> clazz) {
        Validate.notNull(clazz, "type must not be null");
        Map<String, AccessControl> accessControlAnnotationPerField
                = ReflectionUtils.fetchAnnotationForFields(clazz, AccessControl.class);
        Map<String, AccessControlModel> accessControlModelPerField = new HashMap<>();
        accessControlAnnotationPerField.forEach((fieldName, accessControl) -> {
            AccessControlAuthenticatedModel authenticatedModel = AccessControlAuthenticatedModel.fromValue(accessControl.authenticated());
            AccessControlAccessTierModel accessTierModel = AccessControlAccessTierModel.fromAnnotation(accessControl.tier());
            AccessControlScopesModel scopesModel = AccessControlScopesModel.fromAnnotation(accessControl.scopes());
            AccessControlOwnershipModel ownershipModel = AccessControlOwnershipModel.fromAnnotation(accessControl.ownership());
            if (authenticatedModel != null || accessTierModel != null || scopesModel != null || ownershipModel != null) {
                accessControlModelPerField.put(fieldName, AccessControlModel.builder()
                        .authenticated(authenticatedModel)
                        .requiredAccessTier(AccessControlAccessTierModel.fromAnnotation(accessControl.tier()))
                        .requiredScopes(AccessControlScopesModel.fromAnnotation(accessControl.scopes()))
                        .requiredOwnership(AccessControlOwnershipModel.fromAnnotation(accessControl.ownership()))
                        .build());
            }
        });
        return unmodifiableMap(accessControlModelPerField);
    }

    public static AccessControlModel fromClassAnnotation(Class<?> clazz) {
        Validate.notNull(clazz, "type must not be null");
        AccessControl annotation = clazz.getAnnotation(AccessControl.class);
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
        if (higherPrecedence != null
                && higherPrecedence.getRequiredAccessTier() != null
                && higherPrecedence.getRequiredAccessTier().getRequiredAccessTier() != null) {
            resultBuilder.requiredAccessTier(
                    AccessControlAccessTierModel.builder().requiredAccessTier(higherPrecedence.getRequiredAccessTier().getRequiredAccessTier()).build()
            );
        } else if (lowerPrecedence != null
                && lowerPrecedence.getRequiredAccessTier() != null
                && lowerPrecedence.getRequiredAccessTier().getRequiredAccessTier() != null) {
            resultBuilder.requiredAccessTier(
                    AccessControlAccessTierModel.builder().requiredAccessTier(lowerPrecedence.getRequiredAccessTier().getRequiredAccessTier()).build()
            );
        }
        if (higherPrecedence != null
                && higherPrecedence.getRequiredScopes() != null
                && (CollectionUtils.isNotEmpty(higherPrecedence.getRequiredScopes().getRequiredScopes())
                || StringUtils.isNotBlank(higherPrecedence.getRequiredScopes().getRequiredScopesExpression()))) {
            resultBuilder.requiredScopes(
                    AccessControlScopesModel.builder()
                            .requiredScopes(higherPrecedence.getRequiredScopes().getRequiredScopes())
                            .requiredScopesExpression(higherPrecedence.getRequiredScopes().getRequiredScopesExpression())
                            .build()
            );
        } else if (lowerPrecedence != null
                && lowerPrecedence.getRequiredScopes() != null
                && (CollectionUtils.isNotEmpty(lowerPrecedence.getRequiredScopes().getRequiredScopes())
                || StringUtils.isNotBlank(lowerPrecedence.getRequiredScopes().getRequiredScopesExpression()))) {
            resultBuilder.requiredScopes(
                    AccessControlScopesModel.builder()
                            .requiredScopes(lowerPrecedence.getRequiredScopes().getRequiredScopes())
                            .requiredScopesExpression(lowerPrecedence.getRequiredScopes().getRequiredScopesExpression())
                            .build()
            );
        }
        if (higherPrecedence != null
                && higherPrecedence.getRequiredOwnership() != null
                && (StringUtils.isNotBlank(higherPrecedence.getRequiredOwnership().getOwnerIdFieldPath())
                || higherPrecedence.getRequiredOwnership().getOwnerIdExtractor() != null)) {
            resultBuilder.requiredOwnership(
                    AccessControlOwnershipModel.builder()
                            .ownerIdFieldPath(higherPrecedence.getRequiredOwnership().getOwnerIdFieldPath())
                            .ownerIdExtractor(higherPrecedence.getRequiredOwnership().getOwnerIdExtractor())
                            .build()
            );
        } else if (lowerPrecedence != null
                && lowerPrecedence.getRequiredOwnership() != null
                && (StringUtils.isNotBlank(lowerPrecedence.getRequiredOwnership().getOwnerIdFieldPath())
                || lowerPrecedence.getRequiredOwnership().getOwnerIdExtractor() != null)) {
            resultBuilder.requiredOwnership(
                    AccessControlOwnershipModel.builder()
                            .ownerIdFieldPath(lowerPrecedence.getRequiredOwnership().getOwnerIdFieldPath())
                            .ownerIdExtractor(lowerPrecedence.getRequiredOwnership().getOwnerIdExtractor())
                            .build()
            );
        }
        return resultBuilder.build();
    }

}
