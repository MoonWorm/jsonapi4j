package pro.api4.jsonapi4j.model.document;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.net.URI;

/**
 * Represents a JSON:API link object as defined by the
 * <a href="https://jsonapi.org/format/#document-links-link-object">JSON:API specification</a>.
 *
 * <p>A link object can carry a URL ({@code href}), an optional relation type ({@code rel}),
 * a schema URI ({@code describedby}), a human-readable title, a media type, language
 * information ({@code hreflang}), and arbitrary metadata ({@code meta}).
 *
 * @see LinksObject
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
@SuperBuilder
@AllArgsConstructor
public final class LinkObject {

    private final String href;
    private final String rel;
    private final URI describedby;
    private final String title;
    private final String type;
    private final Object hreflang;
    private final Object meta;

}
