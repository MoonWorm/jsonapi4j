package pro.api4.jsonapi4j.plugin.ac;

import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlAccessTierModel;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlAuthenticatedModel;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlOwnershipModel;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlRequirements;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlScopesModel;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

public final class JsonApiAnnotationExtractorUtils {

    private JsonApiAnnotationExtractorUtils() {

    }

    public static AccessControlRequirements extractAccessControlInfo(Class<?> object) {
        AccessControlRequirements classLevelAccessControl = new AccessControlRequirements();

        AccessControl accessControl = object.getAnnotation(AccessControl.class);
        if (accessControl == null) {
            return classLevelAccessControl;
        }

        AccessControlAuthenticatedModel.fromAnnotation(accessControl.authenticated())
                .ifPresent(classLevelAccessControl::setAuthenticated);
        AccessControlAccessTierModel.fromAnnotation(accessControl.tier())
                .ifPresent(classLevelAccessControl::setRequiredAccessTier);
        AccessControlScopesModel.fromAnnotation(accessControl.scopes())
                .ifPresent(classLevelAccessControl::setRequiredScopes);
        AccessControlOwnershipModel.fromAnnotation(accessControl.ownership())
                .ifPresent(classLevelAccessControl::setRequiredOwnership);
        return classLevelAccessControl;
    }

    public static Map<String, AccessControlRequirements> extractAccessControlAnnotationsForFields(Class<?> object) {
        Map<String, AccessControl> accessControlAnnotationPerField
                = ReflectionUtils.fetchAnnotationForFields(object, AccessControl.class);
        return accessControlAnnotationPerField.entrySet().stream()
                .collect(
                        toMap(
                                Map.Entry::getKey,
                                e -> {
                                    AccessControl accessControl = e.getValue();
                                    return new AccessControlRequirements(
                                            AccessControlAuthenticatedModel.fromAnnotation(accessControl.authenticated()).orElse(null),
                                            AccessControlAccessTierModel.fromAnnotation(accessControl.tier()).orElse(null),
                                            AccessControlScopesModel.fromAnnotation(accessControl.scopes()).orElse(null),
                                            AccessControlOwnershipModel.fromAnnotation(accessControl.ownership()).orElse(null)
                                    );
                                }
                        ));
    }

}
