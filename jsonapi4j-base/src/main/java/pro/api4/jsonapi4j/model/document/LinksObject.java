package pro.api4.jsonapi4j.model.document;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.LinkedHashMap;

/**
 * JSON:API specification reference:
 * <a href="https://jsonapi.org/format/#auto-id--link-objects">Links Object</a>
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LinksObject extends LinkedHashMap<String, Object> {

    public static final String SELF_FIELD = "self";
    public static final String RELATED_FIELD = "related";
    public static final String NEXT_FIELD = "next";

    public static LinksObjectBuilder builder() {
        return new LinksObjectBuilder();
    }

    public static class LinksObjectBuilder {

        private final LinksObject links = new LinksObject();

        public LinksObjectBuilder self(String href) {
            links.put(SELF_FIELD, href);
            return this;
        }

        public LinksObjectBuilder next(String href) {
            links.put(NEXT_FIELD, href);
            return this;
        }

        public LinksObjectBuilder related(Object linkObject) {
            links.put(RELATED_FIELD, linkObject);
            return this;
        }

        public LinksObject build() {
            return links;
        }

    }
}
