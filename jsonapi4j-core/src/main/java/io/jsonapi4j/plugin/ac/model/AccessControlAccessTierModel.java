package io.jsonapi4j.plugin.ac.model;

import io.jsonapi4j.plugin.ac.annotation.AccessControlAccessTier;
import lombok.Builder;
import lombok.Getter;

import java.util.Optional;

@Builder
@Getter
public class AccessControlAccessTierModel {

    public static final AccessControlAccessTierModel DEFAULT = AccessControlAccessTierModel.builder().build();

    @Builder.Default
    private String requiredAccessTier = null;

    public static Optional<AccessControlAccessTierModel> fromAnnotation(AccessControlAccessTier annotation) {
        return Optional.ofNullable(annotation)
                .map(AccessControlAccessTier::value)
                .map(at -> AccessControlAccessTierModel.builder().requiredAccessTier(at).build());
    }

}
