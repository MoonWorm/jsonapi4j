package pro.api4.jsonapi4j.plugin.ac.model;

import lombok.Builder;
import lombok.Getter;
import pro.api4.jsonapi4j.plugin.ac.annotation.Authenticated;

import java.util.Optional;

@Builder
@Getter
public class AccessControlAuthenticatedModel {

    public static final AccessControlAuthenticatedModel DEFAULT = AccessControlAuthenticatedModel.builder().build();

    @Builder.Default
    private Authenticated authenticated = Authenticated.ANONYMOUS;

    public static Optional<AccessControlAuthenticatedModel> fromAnnotation(Authenticated authenticated) {
        return Optional.ofNullable(authenticated)
                .map(au -> AccessControlAuthenticatedModel.builder()
                        .authenticated(authenticated)
                        .build()
                );
    }

}
