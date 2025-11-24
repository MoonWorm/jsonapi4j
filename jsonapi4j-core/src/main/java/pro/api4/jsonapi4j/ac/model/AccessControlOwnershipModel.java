package pro.api4.jsonapi4j.ac.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.ac.annotation.AccessControlOwnership;
import pro.api4.jsonapi4j.ac.ownership.OwnerIdExtractor;

@EqualsAndHashCode
@ToString
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
public class AccessControlOwnershipModel {

    private String ownerIdFieldPath;
    private Class<? extends OwnerIdExtractor<?>> ownerIdExtractor;

    static AccessControlOwnershipModel fromAnnotation(AccessControlOwnership annotation) {
        if (annotation == null) {
            return null;
        }
        return AccessControlOwnershipModel.builder()
                .ownerIdFieldPath(StringUtils.isBlank(annotation.ownerIdFieldPath()) ? null : annotation.ownerIdFieldPath())
                .ownerIdExtractor(annotation.ownerIdExtractor())
                .build();
    }

}
