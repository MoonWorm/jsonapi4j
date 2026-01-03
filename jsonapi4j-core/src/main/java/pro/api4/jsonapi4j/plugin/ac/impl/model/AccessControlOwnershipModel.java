package pro.api4.jsonapi4j.plugin.ac.impl.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.plugin.ac.impl.annotation.AccessControlOwnership;
import pro.api4.jsonapi4j.plugin.ac.impl.ownership.NoOpOwnerIdExtractor;
import pro.api4.jsonapi4j.plugin.ac.impl.ownership.OwnerIdExtractor;

@EqualsAndHashCode
@ToString
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
public class AccessControlOwnershipModel {

    private String ownerIdFieldPath;
    private Class<? extends OwnerIdExtractor<?>> ownerIdExtractor;

    static AccessControlOwnershipModel fromAnnotation(AccessControlOwnership annotation) {
        if (annotation == null
                || (!isOwnerIdFieldPathDefined(annotation.ownerIdFieldPath()) && !isOwnerIdExtractorDefined(annotation.ownerIdExtractor()))) {
            return null;
        }
        return AccessControlOwnershipModel.builder()
                .ownerIdFieldPath(isOwnerIdFieldPathDefined(annotation.ownerIdFieldPath()) ? annotation.ownerIdFieldPath() : null)
                .ownerIdExtractor(isOwnerIdExtractorDefined(annotation.ownerIdExtractor()) ? annotation.ownerIdExtractor() : null)
                .build();
    }

    private static boolean isOwnerIdFieldPathDefined(String ownerIdFieldPath) {
        return StringUtils.isNotBlank(ownerIdFieldPath) && !AccessControlOwnership.NOT_SET.equals(ownerIdFieldPath);
    }

    private static boolean isOwnerIdExtractorDefined(Class<? extends OwnerIdExtractor<?>> ownerIdExtractor) {
        return !NoOpOwnerIdExtractor.class.equals(ownerIdExtractor);
    }

}
