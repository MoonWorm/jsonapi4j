package pro.api4.jsonapi4j.sampleapp.domain.user;

import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.exception.JsonApiRequestValidationException;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.RelativeRef.RelationshipType;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The {@code meta} object carried by every {@code relatives} resource linkage, in both directions — the server returns
 * it and clients send it back when writing the relationship.
 * <p>
 * This is the single definition of that shape: the relationship returns it, the operations read and validate incoming
 * linkage through it, and the OAS plugin publishes it as the linkage's {@code meta} schema.
 *
 * @param relationshipType how the linked user is related to the parent one
 */
public record RelativeLinkageMeta(RelationshipType relationshipType) {

    public static final String RELATIONSHIP_TYPE_META_KEY = "relationshipType";

    /**
     * Reads the linkage meta of an incoming resource identifier. Returns empty when the member is absent, and rejects
     * a value that is present but not a known {@link RelationshipType}.
     *
     * @param linkageMeta the raw {@code meta} member, as parsed from the request body
     * @return the typed linkage meta, or empty when none was sent
     */
    public static Optional<RelativeLinkageMeta> fromLinkageMeta(Object linkageMeta) {
        if (!(linkageMeta instanceof Map<?, ?> meta)) {
            return Optional.empty();
        }
        Object rawRelationshipType = meta.get(RELATIONSHIP_TYPE_META_KEY);
        if (rawRelationshipType == null) {
            return Optional.empty();
        }
        if (!(rawRelationshipType instanceof String relationshipType) || StringUtils.isBlank(relationshipType)) {
            throw invalidRelationshipType();
        }
        try {
            return Optional.of(new RelativeLinkageMeta(RelationshipType.valueOf(relationshipType.toUpperCase())));
        } catch (IllegalArgumentException ex) {
            throw invalidRelationshipType();
        }
    }

    private static JsonApiRequestValidationException invalidRelationshipType() {
        return new JsonApiRequestValidationException(
                DefaultErrorCodes.INVALID_ENUM_VALUE,
                String.format(
                        "Meta '%s' object only accepts string values: %s",
                        RELATIONSHIP_TYPE_META_KEY,
                        Arrays.stream(RelationshipType.values()).map(Enum::name).collect(Collectors.joining(", "))
                )
        );
    }

}
