package pro.api4.jsonapi4j.model.document;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.net.URI;

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
