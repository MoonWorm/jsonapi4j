package pro.api4.jsonapi4j.model.document;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import pro.api4.jsonapi4j.model.document.data.JsonApiObject;

/**
 * Abstract base for all JSON:API top-level document types.
 * <p>
 * Carries the three common top-level members — {@code "links"}, {@code "meta"}, and
 * {@code "jsonapi"} — that are present in every JSON:API document regardless of whether
 * it is a resource document, relationship document, error document, or meta-only document.
 * Subclasses add the {@code "data"} or {@code "errors"} member appropriate to the document type.
 *
 * @see <a href="https://jsonapi.org/format/#document-top-level">JSON:API Top-Level Document</a>
 */
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public abstract class BaseDoc {

    /** The JSON:API {@code "links"} member name. */
    public static final String LINKS_FIELD = "links";
    /** The JSON:API {@code "meta"} member name. */
    public static final String META_FIELD = "meta";
    /** The JSON:API {@code "jsonapi"} member name. */
    public static final String JSONAPI_FIELD = "jsonapi";

    /** Top-level document links (e.g. {@code "self"}, {@code "related"}, pagination links). */
    private final LinksObject links;
    /** Top-level document meta; {@code null} omits the member from the serialized output. */
    private final Object meta;
    /** The {@code "jsonapi"} member describing the implemented JSON:API version and negotiated ext/profile. */
    private final JsonApiObject jsonapi;

}
