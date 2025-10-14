package pro.api4.jsonapi4j.plugin.ac;

import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlAccessTier;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlAuthenticated;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlOwnership;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlScopes;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlAccessTierModel;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlAuthenticatedModel;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlRequirements;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlOwnershipModel;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlScopesModel;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public final class JsonApiAnnotationExtractorUtils {

    private JsonApiAnnotationExtractorUtils() {

    }

    public static AccessControlRequirements extractAccessControlInfo(Class<?> object) {
        AccessControlInfo classAnnotations = new AccessControlInfo(
                object.getAnnotation(AccessControlAuthenticated.class),
                object.getAnnotation(AccessControlAccessTier.class),
                object.getAnnotation(AccessControlScopes.class),
                object.getAnnotation(AccessControlOwnership.class)
        );

        AccessControlRequirements classLevelAccessControl = new AccessControlRequirements();
        AccessControlAuthenticatedModel.fromAnnotation(classAnnotations.getAccessControlAuthenticated())
                .ifPresent(classLevelAccessControl::setRequireAuthenticatedUser);
        AccessControlAccessTierModel.fromAnnotation(classAnnotations.getAccessControlAccessTier())
                .ifPresent(classLevelAccessControl::setRequiredAccessTier);
        AccessControlScopesModel.fromAnnotation(classAnnotations.getAccessControlScopes())
                .ifPresent(classLevelAccessControl::setRequiredScopes);
        AccessControlOwnershipModel.fromAnnotation(classAnnotations.getAccessControlOwnership())
                .ifPresent(classLevelAccessControl::setRequiredOwnership);
        return classLevelAccessControl;
    }

    public static Map<String, AccessControlRequirements> extractAccessControlAnnotationsForFields(Class<?> object) {
        Map<String, AccessControlAuthenticated> acessControlAuthenticatedUserMap
                = ReflectionUtils.fetchAnnotationForFields(object, AccessControlAuthenticated.class);
        Map<String, AccessControlAccessTier> acessControlAccessTierMap
                = ReflectionUtils.fetchAnnotationForFields(object, AccessControlAccessTier.class);
        Map<String, AccessControlScopes> acessControlScopesMap
                = ReflectionUtils.fetchAnnotationForFields(object, AccessControlScopes.class);
        Map<String, AccessControlOwnership> accessControlOwnershipMap
                = ReflectionUtils.fetchAnnotationForFields(object, AccessControlOwnership.class);
        return Stream.of(
                        acessControlAuthenticatedUserMap.keySet(),
                        acessControlAccessTierMap.keySet(),
                        acessControlScopesMap.keySet(),
                        accessControlOwnershipMap.keySet()
                ).flatMap(Set::stream)
                .distinct()
                .collect(toMap(
                        f -> f,
                        f -> new AccessControlRequirements(
                                AccessControlAuthenticatedModel.fromAnnotation(acessControlAuthenticatedUserMap.get(f)).orElse(null),
                                AccessControlAccessTierModel.fromAnnotation(acessControlAccessTierMap.get(f)).orElse(null),
                                AccessControlScopesModel.fromAnnotation(acessControlScopesMap.get(f)).orElse(null),
                                AccessControlOwnershipModel.fromAnnotation(accessControlOwnershipMap.get(f)).orElse(null)
                        )
                ));
    }

}
