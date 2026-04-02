package pro.api4.jsonapi4j.model.document.data;

import java.net.URI;
import java.util.List;

public class JsonApiObject {

    public static final String JSONAPI_VERSION_1_0 = "1.0";
    public static final String JSONAPI_VERSION_1_1 = "1.1";

    public static final String VERSION_FIELD = "version";
    public static final String EXT_FIELD = "ext";
    public static final String PROFILE_FIELD = "profile";
    public static final String META_FIELD = "meta";

    private String version;
    private List<URI> ext;
    private List<URI> profile;
    private Object meta;

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
        this.version = version;
        this.ext = ext;
        this.profile = profile;
    }

    public JsonApiObject(List<URI> ext,
                         List<URI> profile) {
        this(JSONAPI_VERSION_1_1, ext, profile);
    }

}
