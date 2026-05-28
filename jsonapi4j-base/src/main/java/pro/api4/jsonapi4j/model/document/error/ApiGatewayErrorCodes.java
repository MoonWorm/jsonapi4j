package pro.api4.jsonapi4j.model.document.error;

/**
 * {@link ErrorCode} constants for API gateway-level errors.
 * <p>
 * Use these codes when the framework or an operation needs to signal a gateway-tier failure
 * (rate limiting, upstream timeout) as a JSON:API error object.
 */
public enum ApiGatewayErrorCodes implements ErrorCode {

    /** The client has exceeded the allowed request rate. */
    RATE_LIMITED("RATE_LIMITED"),
    /** The upstream service did not respond within the allowed time. */
    GATEWAY_TIMEOUT("GATEWAY_TIMEOUT");

    private final String code;

    ApiGatewayErrorCodes(String code) {
        this.code = code;
    }

    @Override
    public String toCode() {
        return code;
    }

}
