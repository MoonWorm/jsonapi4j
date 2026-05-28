package pro.api4.jsonapi4j.request;

import java.net.URI;

/**
 * Request mixin that exposes the JSON:API extension URI negotiated for the current request.
 * <p>
 * Extensions are identified by URIs and advertised in the {@code Content-Type} and
 * {@code Accept} headers as {@code ext} parameters
 * (e.g. {@code application/vnd.api+json;ext="https://jsonapi.org/ext/atomic"}).
 * Implement this interface when an operation needs to inspect which extension the client
 * has requested.
 *
 * @see <a href="https://jsonapi.org/format/#extensions">JSON:API Extensions</a>
 */
public interface ExtensionAwareRequest {

    /**
     * Returns the URI of the JSON:API extension in use for this request,
     * or {@code null} if no extension was negotiated.
     *
     * @return the extension URI, or {@code null}
     */
    URI getExtension();

}
