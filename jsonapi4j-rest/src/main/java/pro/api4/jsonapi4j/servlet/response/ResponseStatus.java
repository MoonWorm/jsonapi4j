package pro.api4.jsonapi4j.servlet.response;

import pro.api4.jsonapi4j.http.HttpStatusCodes;

import java.util.Optional;

public class ResponseStatus {

    private static final ThreadLocal<HttpStatusCodes> RESPONSE_STATUS = new ThreadLocal<>();

    public static void overrideResponseStatus(HttpStatusCodes statusCode) {
        RESPONSE_STATUS.set(statusCode);
    }

    public static Optional<Integer> getOverriddenStatus() {
        Optional<Integer> overriddenStatus = Optional.ofNullable(RESPONSE_STATUS.get()).map(HttpStatusCodes::getCode);
        RESPONSE_STATUS.remove();
        return overriddenStatus;
    }

}
