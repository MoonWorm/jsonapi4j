package pro.api4.jsonapi4j.ac.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.ac.ReflectionUtils;
import pro.api4.jsonapi4j.ac.annotation.AccessControl;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

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
        return builder()
                .authenticated(AccessControlAuthenticatedModel.fromValue(annotation.authenticated()))
                .requiredAccessTier(AccessControlAccessTierModel.fromAnnotation(annotation.tier()))
                .requiredScopes(AccessControlScopesModel.fromAnnotation(annotation.scopes()))
                .requiredOwnership(AccessControlOwnershipModel.fromAnnotation(annotation.ownership()))
                .build();
    }

    public static Map<String, AccessControlModel> fromFieldsAnnotations(Class<?> clazz) {
        Validate.notNull(clazz, "type must not be null");
        Map<String, AccessControl> accessControlAnnotationPerField
                = ReflectionUtils.fetchAnnotationForFields(clazz, AccessControl.class);
        return accessControlAnnotationPerField.entrySet().stream()
                .collect(
                        toMap(
                                Map.Entry::getKey,
                                e -> {
                                    AccessControl accessControl = e.getValue();
                                    return AccessControlModel.builder()
                                            .authenticated(AccessControlAuthenticatedModel.fromValue(accessControl.authenticated()))
                                            .requiredAccessTier(AccessControlAccessTierModel.fromAnnotation(accessControl.tier()))
                                            .requiredScopes(AccessControlScopesModel.fromAnnotation(accessControl.scopes()))
                                            .requiredOwnership(AccessControlOwnershipModel.fromAnnotation(accessControl.ownership()))
                                            .build();
                                }
                        ));
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
