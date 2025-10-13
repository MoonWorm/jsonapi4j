package io.jsonapi4j.model.document.meta;

import lombok.EqualsAndHashCode;
import lombok.ToString;

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
public class MetaDoc {

    private final Object meta;

    public MetaDoc(Object meta) {
        this.meta = meta;
    }

    public Object getMeta() {
        return meta;
    }
}
