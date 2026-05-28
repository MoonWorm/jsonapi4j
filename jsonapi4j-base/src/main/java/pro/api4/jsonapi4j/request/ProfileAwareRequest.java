package pro.api4.jsonapi4j.request;

import java.net.URI;

/**
 * Request mixin that exposes the JSON:API profile URI negotiated for the current request.
 * <p>
 * Profiles are identified by URIs and advertised in the {@code Content-Type} and
 * {@code Accept} headers as {@code profile} parameters
 * (e.g. {@code application/vnd.api+json;profile="https://example.com/profiles/timestamps"}).
 * Implement this interface when an operation needs to inspect which profile the client
 * has requested.
 *
 * @see <a href="https://jsonapi.org/format/#profiles">JSON:API Profiles</a>
 */
public interface ProfileAwareRequest {

    /**
     * Returns the URI of the JSON:API profile in use for this request,
     * or {@code null} if no profile was negotiated.
     *
     * @return the profile URI, or {@code null}
     */
    URI getProfile();

}
