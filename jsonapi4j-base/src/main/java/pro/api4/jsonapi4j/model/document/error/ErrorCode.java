package pro.api4.jsonapi4j.model.document.error;

/**
 * Contract for a machine-readable error code included in a JSON:API error object's
 * {@code "code"} member.
 * <p>
 * Applications and the framework define error codes as enums (or other types) that implement
 * this interface, ensuring type-safe usage throughout the exception hierarchy and error handlers.
 * Built-in implementations include {@link DefaultErrorCodes}, {@link AuthErrorCodes}, and
 * {@link ApiGatewayErrorCodes}.
 *
 * @see pro.api4.jsonapi4j.exception.JsonApi4jException
 * @see DefaultErrorCodes
 */
public interface ErrorCode {

    /**
     * Returns the string code included in the JSON:API {@code "code"} member of an error object.
     *
     * @return the error code string; must not be {@code null}
     */
    String toCode();

}
