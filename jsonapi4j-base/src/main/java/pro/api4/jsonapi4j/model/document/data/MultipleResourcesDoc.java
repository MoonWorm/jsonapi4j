package pro.api4.jsonapi4j.model.document.data;

import pro.api4.jsonapi4j.model.document.LinksObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * Represents <a href="https://jsonapi.org/format/#document-top-level">Top-level Document</a> for 'data'
 * scenarios targeting multiple primary resources.
 * <p>
 * For example:
 * <pre>
 *     {@code
 *     {
 *          "data": [
 *              {
 *                  "attributes": {
 *                      "firstName": "Jack",
 *                      "lastName": "Doe",
 *                      "email": "jack@doe.com"
 *                  },
 *                  "relationships": {
 *                      "citizenships": {
 *                          "links": {
 *                              "self": "/users/3/relationships/citizenships"
 *                          }
 *                      }
 *                  },
 *                  "links": {
 *                      "self": "/users/3"
 *                  },
 *                  "id": "3",
 *                  "type": "users"
 *              },
 *              {
 *                  "attributes": {
 *                      "firstName": "Jessy",
 *                      "lastName": "Doe",
 *                      "email": "jessy@doe.com"
 *                  },
 *                  "relationships": {
 *                      "citizenships": {
 *                          "links": {
 *                              "self": "/users/4/relationships/citizenships"
 *                          }
 *                      }
 *                  },
 *                  "links": {
 *                      "self": "/users/4"
 *                  },
 *                  "id": "4",
 *                  "type": "users"
 *              }
 *          ],
 *          "links": {
 *              "self": "/users?page%5Bcursor%5D=DoJu",
 *              "next": "/users?page%5Bcursor%5D=DoJw"
 *          }
 *      }
 *     }
 * </pre>
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MultipleResourcesDoc<PRIMARY_RESOURCE extends ResourceObject<?, ?>>
        extends AbstractMultipleDataItemsDoc<PRIMARY_RESOURCE> {

    public MultipleResourcesDoc(List<PRIMARY_RESOURCE> data,
                                LinksObject links,
                                Object meta,
                                List<? extends ResourceObject<?, ?>> included,
                                JsonApiObject jsonapi) {
        super(data, links, meta, included, jsonapi);
    }

    public MultipleResourcesDoc(List<PRIMARY_RESOURCE> data,
                                LinksObject links,
                                Object meta,
                                List<? extends ResourceObject<?, ?>> included) {
        this(data, links, meta, included, null);
    }

    public MultipleResourcesDoc(List<PRIMARY_RESOURCE> data,
                                LinksObject links,
                                Object meta) {
        this(data, links, meta, null);
    }

    public MultipleResourcesDoc(List<PRIMARY_RESOURCE> data,
                                LinksObject links) {
        this(data, links, null);
    }

    public MultipleResourcesDoc(List<PRIMARY_RESOURCE> data) {
        this(data, null);
    }

    public MultipleResourcesDoc(List<PRIMARY_RESOURCE> data,
                                Object meta) {
        this(data, null, meta);
    }

    public MultipleResourcesDoc() {
        this(null);
    }

}
