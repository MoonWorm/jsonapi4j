package pro.api4.jsonapi4j.model.document.error;

public enum AuthErrorCodes implements ErrorCode {

    UNAUTHORIZED("UNAUTHORIZED"),
    ACCESS_TOKEN_REVOKED("ACCESS_TOKEN_REVOKED"),
    ACCESS_TOKEN_EXPIRED("ACCESS_TOKEN_EXPIRED"),
    FORBIDDEN("FORBIDDEN"),
    INSUFFICIENT_SCOPES("INSUFFICIENT_SCOPES"),
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
