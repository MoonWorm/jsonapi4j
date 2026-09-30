package pro.api4.jsonapi4j.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import pro.api4.jsonapi4j.http.HttpStatusCodes;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The request asks for {@code include} paths the server does not support - a relationship name the resource type
 * doesn't have, or a path deeper than supported. A client error the JSON:API specification answers with
 * {@code 400 Bad Request}: the request has to change, a retry returns the same.
 *
 * <p>Rendered with one error object per path, each naming its path in {@code meta.path} so a caller - e.g. a compound
 * documents resolver fetching from this server - can tell which includes were rejected.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class UnsupportedIncludeException extends JsonApi4jException {

    /**
     * The {@code meta} member naming the rejected include path.
     */
    public static final String PATH_META_FIELD = "path";

    private final List<UnsupportedInclude> unsupportedIncludes;

    public UnsupportedIncludeException(List<UnsupportedInclude> unsupportedIncludes) {
        super(
                HttpStatusCodes.SC_400_BAD_REQUEST.getCode(),
                DefaultErrorCodes.UNSUPPORTED_INCLUDE,
                unsupportedIncludes.stream().map(UnsupportedInclude::detail).collect(Collectors.joining("; "))
        );
        this.unsupportedIncludes = List.copyOf(unsupportedIncludes);
    }

    public UnsupportedIncludeException(String path, String detail) {
        this(List.of(new UnsupportedInclude(path, detail)));
    }

    /**
     * @param path   the rejected include path, as requested
     * @param detail why it is not supported
     */
    public record UnsupportedInclude(String path, String detail) {
    }

}
