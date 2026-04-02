package pro.api4.jsonapi4j.model.document.meta;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import pro.api4.jsonapi4j.model.document.BaseDoc;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.JsonApiObject;

/**
 * Represents <a href="https://jsonapi.org/format/#document-top-level">Top-level Document</a> for 'meta'-only scenarios.
 * <p>
 * For example:
 * <pre>
 *     {@code
 *     {
 *         "meta": {
 *             "health": "UP"
 *         }
 *     }
 *     }
 * </pre>
 * <p>
 * It's never used in jsonapi4j framework but can be used as a dedicated model if needed.
 */
@ToString
@EqualsAndHashCode
public class MetaDoc extends BaseDoc {

    public MetaDoc(LinksObject links,
                   Object meta,
                   JsonApiObject jsonapi) {
        super(links, meta, jsonapi);
    }

    public MetaDoc(LinksObject links) {
        this(links, null, null);
    }

}
