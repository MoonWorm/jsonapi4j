package io.jsonapi4j.model.document.data;

import io.jsonapi4j.model.document.LinksObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Represents <a href="https://jsonapi.org/format/#document-top-level">Top-level Document</a> for 'data'
 * scenarios targeting to-one relationship.
 * <p>
 * For example:
 * <pre>
 *     {@code
 *     {
 *          "data": {
 *              "id": "NO",
 *              "type": "countries"
 *          },
 *          "links": {
 *              "self": "/users/1/relationships/placeOfBirth",
 *              "related": {
 *                  "countries": "/countries?filter[id]=NO"
 *              }
 *          }
 *      }
 *     }
 * </pre>
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ToOneRelationshipDoc extends AbstractSingleDataItemDoc<ResourceIdentifierObject> {

    public ToOneRelationshipDoc(ResourceIdentifierObject data,
                                LinksObject links, Object meta) {
        super(data, links, meta);
    }

    public ToOneRelationshipDoc(ResourceIdentifierObject data,
                                LinksObject links) {
        this(data, links, null);
    }

    public ToOneRelationshipDoc(LinksObject links) {
        this(null, links, null);
    }

    public ToOneRelationshipDoc(ResourceIdentifierObject data) {
        this(data, null, null);
    }

    public ToOneRelationshipDoc() {
        this(null, null);
    }

    public static ToOneRelationshipDoc fromBaseDoc(ResourceIdentifierObject data,
                                                   BaseDoc base) {
        return new ToOneRelationshipDoc(data, base.getLinks(), base.getMeta());
    }



}
