package pro.api4.jsonapi4j.model.document;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import pro.api4.jsonapi4j.model.document.data.JsonApiObject;

/**
 * Carries common members ('links' and 'meta') that exist for all types of JSON:API data documents regardless of their
 * nature (all types of relationship & resource docs, error docs, meta doc).
 */
@ToString
@EqualsAndHashCode
public abstract class BaseDoc {

    public static final String LINKS_FIELD = "links";
    public static final String META_FIELD = "meta";
    public static final String JSONAPI_FIELD = "jsonapi";

    private LinksObject links;
    private Object meta;
    private JsonApiObject jsonapi;

    public BaseDoc(LinksObject links,
                   Object meta,
                   JsonApiObject jsonapi) {
        this.links = links;
        this.meta = meta;
        this.jsonapi = jsonapi;
    }

    public LinksObject getLinks() {
        return links;
    }

    public Object getMeta() {
        return meta;
    }

    public JsonApiObject getJsonapi() {
        return jsonapi;
    }

}
