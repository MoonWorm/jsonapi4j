package pro.api4.jsonapi4j.plugin.ac.model;

import org.apache.commons.lang3.Validate;
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
        Validate.notNull(clazz);
        AccessControlRequirements classLevelAccessControl
                = JsonApiAnnotationExtractorUtils.extractAccessControlInfo(clazz);
        Map<String, AccessControlRequirements> fieldLevelAccessControl
                = JsonApiAnnotationExtractorUtils.extractAccessControlAnnotationsForFields(clazz);
        return new AccessControlRequirementsForObject(classLevelAccessControl, fieldLevelAccessControl);
    }

    public static AccessControlRequirementsForObject merge(AccessControlRequirementsForObject master,
                                                           AccessControlRequirementsForObject other) {
        AccessControlRequirements classLevelAccessControl = AccessControlRequirements.merge(
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
                        field -> AccessControlRequirements.merge(
                                isNotNull(master, field) ? master.getFieldLevel().get(field) : null,
                                isNotNull(other, field) ? other.getFieldLevel().get(field) : null
                        )
                )
        );
        fieldLevelAccessControl = MapUtils.isEmpty(fieldLevelAccessControl) ? null : fieldLevelAccessControl;

        return new AccessControlRequirementsForObject(classLevelAccessControl, fieldLevelAccessControl);
    }

    private static boolean isNotNull(AccessControlRequirementsForObject accessControlRequirementsForObject, String fieldName) {
        return accessControlRequirementsForObject != null
                && accessControlRequirementsForObject.getFieldLevel() != null
                && accessControlRequirementsForObject.getFieldLevel().get(fieldName) != null;
    }

}

