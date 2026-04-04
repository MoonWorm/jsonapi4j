package pro.api4.jsonapi4j.model.document;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

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
            this.links.put(SELF_FIELD, href);
            return this;
        }

        public LinksObjectBuilder next(String href) {
            this.links.put(NEXT_FIELD, href);
            return this;
        }

        public LinksObjectBuilder related(Object linkObject) {
            this.links.put(RELATED_FIELD, linkObject);
            return this;
        }

        public LinksObjectBuilder linkObject(String linkName, Object linkObject) {
            if (StringUtils.isNotBlank(linkName)) {
                this.links.put(linkName, linkObject);
            }
            return this;
        }

        public LinksObjectBuilder href(String linkName, String href) {
            if (StringUtils.isNotBlank(linkName)) {
                this.links.put(linkName, href);
            }
            return this;
        }

        public LinksObjectBuilder putAll(Map<String, ?> links) {
            if (links != null) {
                this.links.putAll(links);
            }
            return this;
        }

        public LinksObject build() {
            return links;
        }

    }
}
