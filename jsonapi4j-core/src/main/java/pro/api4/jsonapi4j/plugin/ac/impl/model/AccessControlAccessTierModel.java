package pro.api4.jsonapi4j.plugin.ac.impl.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import pro.api4.jsonapi4j.plugin.ac.impl.annotation.AccessControlAccessTier;

@EqualsAndHashCode
@ToString
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
public class AccessControlAccessTierModel {

    private String requiredAccessTier;

    static AccessControlAccessTierModel fromAnnotation(AccessControlAccessTier annotation) {
        if (annotation == null || AccessControlAccessTier.NOT_SET.equals(annotation.value())) {
            return null;
        }
        return AccessControlAccessTierModel.builder().requiredAccessTier(annotation.value()).build();
    }

}
