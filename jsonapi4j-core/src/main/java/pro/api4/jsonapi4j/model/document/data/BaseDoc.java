package pro.api4.jsonapi4j.model.document.data;

import pro.api4.jsonapi4j.model.document.LinksObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Carries common members ('links' and 'meta') that exist for all types of JSON:API data documents regardless of their
 * nature (all types of relationship & resource docs).
 * <p>
 * "data" member can be different for JSON:API documents, for example:
 * <ul>
 *     <li>Can be either array or a single object</li>
 *     <li>Data item can be either JSON:API Resource of JSON:API Resource Linkage</li>
 * </ul>
 * This class is not abstract because it can be used independently for composing documents for 'default'
 * relationships that might not have 'data' member at all.
 */
@ToString
@EqualsAndHashCode
public class BaseDoc {

    private LinksObject links;
    private Object meta;

    public BaseDoc(LinksObject links, Object meta) {
        this.links = links;
        this.meta = meta;
    }

    public BaseDoc(LinksObject links) {
        this.links = links;
    }

    public LinksObject getLinks() {
        return links;
    }

    public Object getMeta() {
        return meta;
    }

}
