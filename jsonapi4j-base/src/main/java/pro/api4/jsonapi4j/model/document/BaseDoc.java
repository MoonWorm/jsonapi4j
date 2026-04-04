package pro.api4.jsonapi4j.model.document;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import pro.api4.jsonapi4j.model.document.data.JsonApiObject;

/**
 * Carries common members ('links' and 'meta') that exist for all types of JSON:API data documents regardless of their
 * nature (all types of relationship & resource docs, error docs, meta doc).
 */
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public abstract class BaseDoc {

    public static final String LINKS_FIELD = "links";
    public static final String META_FIELD = "meta";
    public static final String JSONAPI_FIELD = "jsonapi";

    private final LinksObject links;
    private final Object meta;
    private final JsonApiObject jsonapi;

}
