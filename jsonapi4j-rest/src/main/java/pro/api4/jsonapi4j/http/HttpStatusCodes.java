package pro.api4.jsonapi4j.http;

public enum HttpStatusCodes {

    SC_400_BAD_REQUEST(400, "Bad request. Ensure the client composed and sent a proper HTTP request (query parameters, headers, request body, etc.)."),
    SC_403_FORBIDDEN(403, "Forbidden. The requested operation is recognized but not supported by the server."),
    SC_404_RESOURCE_NOT_FOUND(404, "Resource not found. The requested operation or resource is not found."),
    SC_405_METHOD_NOT_SUPPORTED(405, "Method is not supported. Ensure a proper HTTP method for an HTTP request is used."),
    SC_406_NOT_ACCEPTABLE(406, "Not acceptable. The server doesn't support any of the requested by client acceptable content types."),
    SC_409_CONFLICT(409, "Conflict. The requested resource already exists or conflicting with the other existing resources."),
    SC_415_UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type. The JSON:API is using content negotiation. Ensure the proper media type is set into 'Content-Type' header."),
    SC_500_INTERNAL_SERVER_ERROR(500, "Internal Server Error. Something went wrong on the server party."),
    SC_502_BAD_GATEWAY_ERROR(502, "Bad Gateway. Error with a downstream service.");

    private final int code;
    private final String description;

    HttpStatusCodes(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

}
