package io.jsonapi4j.model.document.error;

public enum ApiGatewayErrorCodes implements ErrorCode {

    RATE_LIMITED("RATE_LIMITED"),
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
