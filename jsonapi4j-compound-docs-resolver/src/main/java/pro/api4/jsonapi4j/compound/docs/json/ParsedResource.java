package pro.api4.jsonapi4j.compound.docs.json;

import pro.api4.jsonapi4j.processor.IdAndType;

/**
 * A resource object of a JSON:API document's primary data, parsed once: its identity and relationship linkage, and the
 * resource object itself.
 *
 * @param linkage its identity and relationship linkage; the identity is {@code null} for a resource object without a
 *                textual {@code type} and {@code id}
 * @param json    the resource object as it appears in {@code data}
 */
public record ParsedResource(ResourceLinkage linkage, String json) {

    /**
     * @return the resource's {@code type} and {@code id}, or {@code null} when the resource object lacks them
     */
    public IdAndType idAndType() {
        return linkage.idAndType();
    }

}
