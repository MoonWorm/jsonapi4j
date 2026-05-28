package pro.api4.jsonapi4j.request;


/**
 * Composite request mixin that exposes both cursor-based and limit-offset pagination parameters.
 * <p>
 * Implement this interface (typically alongside {@link JsonApiRequest}) when an operation
 * supports pagination via the JSON:API {@code page[*]} query parameter family.
 * The framework uses the concrete strategy provided by the {@link PaginationMode} that the
 * operation's data supplier returns in its {@link pro.api4.jsonapi4j.response.PaginationAwareResponse}.
 *
 * @see LimitOffsetAwareRequest
 * @see CursorAwareRequest
 */
public interface PaginationAwareRequest extends LimitOffsetAwareRequest, CursorAwareRequest {

    static boolean isJsonApiPaginationParam(String paramName) {
        if (paramName == null) {
            return false;
        }
        String regex = "^page\\[.+]$";
        return paramName.matches(regex);
    }

}
