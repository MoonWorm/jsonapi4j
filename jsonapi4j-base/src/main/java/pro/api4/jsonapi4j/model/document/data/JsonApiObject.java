package pro.api4.jsonapi4j.model.document.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.net.URI;
import java.util.List;

/**
 * Represents the JSON:API {@code "jsonapi"} top-level member of a document.
 * <p>
 * When present, this object describes the highest JSON:API version supported by the server
 * for the current response, and lists any extensions ({@code "ext"}) and profiles
 * ({@code "profile"}) that were applied. It may also carry an arbitrary {@code "meta"} object.
 *
 * @see <a href="https://jsonapi.org/format/#document-jsonapi-object">JSON:API Object</a>
 */
@Getter
@EqualsAndHashCode
@ToString
public class JsonApiObject {

    /** JSON:API specification version string for version 1.0. */
    public static final String JSONAPI_VERSION_1_0 = "1.0";
    /** JSON:API specification version string for version 1.1. */
    public static final String JSONAPI_VERSION_1_1 = "1.1";

    /** The JSON:API {@code "version"} member name. */
    public static final String VERSION_FIELD = "version";
    /** The JSON:API {@code "ext"} member name. */
    public static final String EXT_FIELD = "ext";
    /** The JSON:API {@code "profile"} member name. */
    public static final String PROFILE_FIELD = "profile";
    /** The JSON:API {@code "meta"} member name. */
    public static final String META_FIELD = "meta";

    /** The JSON:API version implemented by the server (e.g. {@code "1.1"}). */
    private final String version;
    /** URIs of JSON:API extensions applied to this response; {@code null} or empty omits the member. */
    private final List<URI> ext;
    /** URIs of JSON:API profiles applied to this response; {@code null} or empty omits the member. */
    private final List<URI> profile;
    /** Optional meta information about the JSON:API object itself; {@code null} omits the member. */
    private final Object meta;

    public JsonApiObject(String version,
                         List<URI> ext,
                         List<URI> profile,
                         Object meta) {
        this.version = version;
        this.ext = ext;
        this.profile = profile;
        this.meta = meta;
    }

    public JsonApiObject(List<URI> ext,
                         List<URI> profile,
                         Object meta) {
        this(JSONAPI_VERSION_1_1, ext, profile, meta);
    }

    public JsonApiObject(String version,
                         List<URI> ext,
                         List<URI> profile) {
        this(version, ext, profile, null);
    }

    public JsonApiObject(List<URI> ext,
                         List<URI> profile) {
        this(JSONAPI_VERSION_1_1, ext, profile);
    }

}
