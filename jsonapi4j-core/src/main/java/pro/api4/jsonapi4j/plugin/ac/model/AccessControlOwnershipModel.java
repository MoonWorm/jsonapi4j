package pro.api4.jsonapi4j.plugin.ac.model;

import pro.api4.jsonapi4j.plugin.ac.ownership.OwnerIdExtractor;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlOwnership;
import lombok.Builder;
import lombok.Getter;

import java.util.Optional;

@Builder
@Getter
public class AccessControlOwnershipModel {

    public static final AccessControlOwnershipModel DEFAULT = AccessControlOwnershipModel.builder().build();

    @Builder.Default
    private String ownerIdFieldPath = "";

    @Builder.Default
    private Class<? extends OwnerIdExtractor<?>> ownerIdExtractor = null;

    public static Optional<AccessControlOwnershipModel> fromAnnotation(AccessControlOwnership accessControlOwnership) {
        return Optional.ofNullable(accessControlOwnership).map(a ->
                AccessControlOwnershipModel.builder()
                        .ownerIdFieldPath(a.ownerIdFieldPath())
                        .ownerIdExtractor(a.ownerIdExtractor())
                        .build()
        );
    }

}
