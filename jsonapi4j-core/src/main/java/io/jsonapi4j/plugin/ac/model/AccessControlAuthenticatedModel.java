package io.jsonapi4j.plugin.ac.model;

import io.jsonapi4j.plugin.ac.annotation.AccessControlAuthenticated;
import lombok.Builder;
import lombok.Getter;

import java.util.Optional;

@Builder
@Getter
public class AccessControlAuthenticatedModel {

    public static final AccessControlAuthenticatedModel DEFAULT = AccessControlAuthenticatedModel.builder().build();

    @Builder.Default
    private boolean requireAuthentication = false;

    public static Optional<AccessControlAuthenticatedModel> fromAnnotation(AccessControlAuthenticated annotation) {
        return Optional.ofNullable(annotation)
                .map(au -> AccessControlAuthenticatedModel.builder()
                        .requireAuthentication(true)
                        .build()
                );
    }

}
