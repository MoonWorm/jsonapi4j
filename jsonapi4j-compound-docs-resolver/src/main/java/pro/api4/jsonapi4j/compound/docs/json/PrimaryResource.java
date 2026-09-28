package pro.api4.jsonapi4j.compound.docs.json;

/**
 * A resource object from the primary data of a primary resource document.
 *
 * @param linkage its identity and relationship linkage
 * @param json    the resource object as it appears in {@code data}
 */
public record PrimaryResource(ResourceLinkage linkage, String json) {
}
