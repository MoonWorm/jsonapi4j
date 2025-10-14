package pro.api4.jsonapi4j.plugin.ac.model;

import pro.api4.jsonapi4j.plugin.ac.JsonApiAnnotationExtractorUtils;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Data
@Builder
public class AccessControlRequirementsForObject {

    private final AccessControlRequirements objectLevel;
    private final Map<String, AccessControlRequirements> fieldLevel;

    public static AccessControlRequirementsForObject DEFAULT = AccessControlRequirementsForObject.builder()
            .objectLevel(AccessControlRequirements.DEFAULT)
            .fieldLevel(Collections.emptyMap())
            .build();

    public static AccessControlRequirementsForObject fromAnnotationsForClass(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Annotated type should not be null");
        }
        AccessControlRequirements classLevelAccessControl
                = JsonApiAnnotationExtractorUtils.extractAccessControlInfo(clazz);
        Map<String, AccessControlRequirements> fieldLevelAccessControl
                = JsonApiAnnotationExtractorUtils.extractAccessControlAnnotationsForFields(clazz);
        return new AccessControlRequirementsForObject(classLevelAccessControl, fieldLevelAccessControl);
    }

    public static AccessControlRequirementsForObject merge(AccessControlRequirementsForObject master,
                                                           AccessControlRequirementsForObject other) {
        AccessControlRequirements classLevelAccessControl = merge(
                master != null ? master.getObjectLevel() : null,
                other != null ? other.getObjectLevel() : null
        );
        @SuppressWarnings("DataFlowIssue")
        Map<String, AccessControlRequirements> fieldLevelAccessControl = Stream.concat(
                master != null && master.getFieldLevel() != null ? master.getFieldLevel().keySet().stream() : Stream.empty(),
                other != null && other.getFieldLevel() != null ? other.getFieldLevel().keySet().stream() : Stream.empty()
        ).filter(
                field -> isNotNull(master, field) || isNotNull(other, field)
        ).collect(
                Collectors.toMap(
                        field -> field,
                        field -> merge(
                                isNotNull(master, field) ? master.getFieldLevel().get(field) : null,
                                isNotNull(other, field) ? other.getFieldLevel().get(field) : null
                        )
                )
        );
        fieldLevelAccessControl = MapUtils.isEmpty(fieldLevelAccessControl) ? null : fieldLevelAccessControl;

        return new AccessControlRequirementsForObject(classLevelAccessControl, fieldLevelAccessControl);
    }

    private static AccessControlRequirements merge(AccessControlRequirements master, AccessControlRequirements other) {
        if (master == null && other == null) {
            return null;
        }
        AccessControlRequirements result = new AccessControlRequirements();
        if (other != null && other.getRequireAuthenticatedUser() != null) {
            result.setRequireAuthenticatedUser(
                    new AccessControlAuthenticatedModel(other.getRequireAuthenticatedUser().isRequireAuthentication())
            );
        } else if (master != null && master.getRequireAuthenticatedUser() != null) {
            result.setRequireAuthenticatedUser(
                    new AccessControlAuthenticatedModel(master.getRequireAuthenticatedUser().isRequireAuthentication())
            );
        }
        if (other != null && other.getRequiredAccessTier() != null) {
            result.setRequiredAccessTier(
                    new AccessControlAccessTierModel(other.getRequiredAccessTier().getRequiredAccessTier())
            );
        } else if (master != null && master.getRequiredAccessTier() != null) {
            result.setRequiredAccessTier(
                    new AccessControlAccessTierModel(master.getRequiredAccessTier().getRequiredAccessTier())
            );
        }
        if (other != null && other.getRequiredScopes() != null) {
            result.setRequiredScopes(
                    new AccessControlScopesModel(
                            other.getRequiredScopes().getRequiredScopes(),
                            other.getRequiredScopes().getRequiredScopesExpression()
                    )
            );
        } else if (master != null && master.getRequiredScopes() != null) {
            result.setRequiredScopes(
                    new AccessControlScopesModel(
                            master.getRequiredScopes().getRequiredScopes(),
                            master.getRequiredScopes().getRequiredScopesExpression()
                    )
            );
        }
        if (other != null && other.getRequiredOwnership() != null) {
            result.setRequiredOwnership(
                    new AccessControlOwnershipModel(
                            other.getRequiredOwnership().getOwnerIdFieldPath(),
                            other.getRequiredOwnership().getOwnerIdExtractor()
                    )
            );
        } else if (master != null && master.getRequiredOwnership() != null) {
            result.setRequiredOwnership(
                    new AccessControlOwnershipModel(
                            master.getRequiredOwnership().getOwnerIdFieldPath(),
                            master.getRequiredOwnership().getOwnerIdExtractor()
                    )
            );
        }
        return result;
    }

    private static boolean isNotNull(AccessControlRequirementsForObject accessControlRequirementsForObject, String fieldName) {
        return accessControlRequirementsForObject != null
                && accessControlRequirementsForObject.getFieldLevel() != null
                && accessControlRequirementsForObject.getFieldLevel().get(fieldName) != null;
    }

}

