package pro.api4.jsonapi4j.model.document.error;

/**
 * {@link ErrorCode} constants for authentication and authorization errors.
 * <p>
 * Used by the Access Control plugin and operation implementations when reporting
 * authentication or authorization failures as JSON:API error objects.
 */
public enum AuthErrorCodes implements ErrorCode {

    /** The request requires authentication but no valid credentials were provided. */
    UNAUTHORIZED("UNAUTHORIZED"),
    /** The access token has been explicitly revoked. */
    ACCESS_TOKEN_REVOKED("ACCESS_TOKEN_REVOKED"),
    /** The access token has expired and a new one must be obtained. */
    ACCESS_TOKEN_EXPIRED("ACCESS_TOKEN_EXPIRED"),
    /** The authenticated principal lacks permission to perform the requested operation. */
    FORBIDDEN("FORBIDDEN"),
    /** The principal's token does not include the required OAuth scopes. */
    INSUFFICIENT_SCOPES("INSUFFICIENT_SCOPES"),
    /** The principal's access tier is below the minimum required for the requested operation. */
    INSUFFICIENT_ACCESS_TIER("INSUFFICIENT_ACCESS_TIER");

    private final String code;

    AuthErrorCodes(String code) {
        this.code = code;
    }

    @Override
    public String toCode() {
        return code;
    }

}
