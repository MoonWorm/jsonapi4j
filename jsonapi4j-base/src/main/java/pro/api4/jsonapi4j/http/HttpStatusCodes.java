package pro.api4.jsonapi4j.http;

import lombok.Getter;

@Getter
public enum HttpStatusCodes {

    // 1xx Informational
    SC_100_CONTINUE(100, "Continue. The server has received the request headers and the client should proceed to send the request body."),
    SC_101_SWITCHING_PROTOCOLS(101, "Switching Protocols. The server is switching protocols as requested by the client."),
    SC_102_PROCESSING(102, "Processing. The server has received and is processing the request, but no response is available yet."),
    SC_103_EARLY_HINTS(103, "Early Hints. The server is sending preliminary response headers before the final response."),

    // 2xx Success
    SC_200_OK(200, "OK. The request has succeeded."),
    SC_201_CREATED(201, "Created. The request has been fulfilled and a new resource has been created."),
    SC_202_ACCEPTED(202, "Accepted. The request has been accepted for processing, but the processing has not been completed."),
    SC_203_NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information. The returned metadata is not exactly the same as available from the origin server."),
    SC_204_NO_CONTENT(204, "No Content. The server has successfully fulfilled the request and there is no content to send in the response body."),
    SC_205_RESET_CONTENT(205, "Reset Content. The server has fulfilled the request and the client should reset the document view."),
    SC_206_PARTIAL_CONTENT(206, "Partial Content. The server is delivering only part of the resource due to a range header sent by the client."),
    SC_207_MULTI_STATUS(207, "Multi-Status. The message body contains multiple status codes for multiple independent operations."),
    SC_208_ALREADY_REPORTED(208, "Already Reported. The members of a DAV binding have already been enumerated in a preceding part of the response."),
    SC_226_IM_USED(226, "IM Used. The server has fulfilled a request for the resource and the response is a representation of the result of one or more instance-manipulations."),

    // 3xx Redirection
    SC_300_MULTIPLE_CHOICES(300, "Multiple Choices. The request has more than one possible response."),
    SC_301_MOVED_PERMANENTLY(301, "Moved Permanently. The requested resource has been permanently moved to a new URI."),
    SC_302_FOUND(302, "Found. The requested resource temporarily resides under a different URI."),
    SC_303_SEE_OTHER(303, "See Other. The response to the request can be found under a different URI using a GET method."),
    SC_304_NOT_MODIFIED(304, "Not Modified. The resource has not been modified since the last request."),
    SC_305_USE_PROXY(305, "Use Proxy. The requested resource must be accessed through the proxy given by the Location header."),
    SC_307_TEMPORARY_REDIRECT(307, "Temporary Redirect. The request should be repeated with another URI but future requests should still use the original URI."),
    SC_308_PERMANENT_REDIRECT(308, "Permanent Redirect. The request and all future requests should be repeated using another URI."),

    // 4xx Client Errors
    SC_400_BAD_REQUEST(400, "Bad request. Ensure the client composed and sent a proper HTTP request (query parameters, headers, request body, etc.)."),
    SC_401_UNAUTHORIZED(401, "Unauthorized. The request requires user authentication."),
    SC_402_PAYMENT_REQUIRED(402, "Payment Required. Reserved for future use."),
    SC_403_FORBIDDEN(403, "Forbidden. The server understood the request but refuses to authorize it."),
    SC_404_RESOURCE_NOT_FOUND(404, "Resource not found. The requested operation or resource is not found."),
    SC_405_METHOD_NOT_SUPPORTED(405, "Method is not supported. Ensure a proper HTTP method for an HTTP request is used."),
    SC_406_NOT_ACCEPTABLE(406, "Not acceptable. The server doesn't support any of the requested by client acceptable content types."),
    SC_407_PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required. The client must first authenticate itself with the proxy."),
    SC_408_REQUEST_TIMEOUT(408, "Request Timeout. The server timed out waiting for the request."),
    SC_409_CONFLICT(409, "Conflict. The requested resource already exists or conflicting with the other existing resources."),
    SC_410_GONE(410, "Gone. The requested resource is no longer available and no forwarding address is known."),
    SC_411_LENGTH_REQUIRED(411, "Length Required. The server requires a Content-Length header in the request."),
    SC_412_PRECONDITION_FAILED(412, "Precondition Failed. One or more conditions in the request header fields evaluated to false."),
    SC_413_CONTENT_TOO_LARGE(413, "Content Too Large. The request entity is larger than the server is willing or able to process."),
    SC_414_URI_TOO_LONG(414, "URI Too Long. The request URI is longer than the server is willing to interpret."),
    SC_415_UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type. The JSON:API is using content negotiation. Ensure the proper media type is set into 'Content-Type' header."),
    SC_416_RANGE_NOT_SATISFIABLE(416, "Range Not Satisfiable. The range specified in the Range header cannot be fulfilled."),
    SC_417_EXPECTATION_FAILED(417, "Expectation Failed. The expectation given in the Expect request header could not be met."),
    SC_418_IM_A_TEAPOT(418, "I'm a teapot. The server refuses to brew coffee because it is, permanently, a teapot."),
    SC_421_MISDIRECTED_REQUEST(421, "Misdirected Request. The request was directed at a server that is not able to produce a response."),
    SC_422_UNPROCESSABLE_CONTENT(422, "Unprocessable Content. The server understands the content type but was unable to process the contained instructions."),
    SC_423_LOCKED(423, "Locked. The resource that is being accessed is locked."),
    SC_424_FAILED_DEPENDENCY(424, "Failed Dependency. The request failed because it depended on another request that failed."),
    SC_425_TOO_EARLY(425, "Too Early. The server is unwilling to risk processing a request that might be replayed."),
    SC_426_UPGRADE_REQUIRED(426, "Upgrade Required. The server refuses to perform the request using the current protocol."),
    SC_428_PRECONDITION_REQUIRED(428, "Precondition Required. The origin server requires the request to be conditional."),
    SC_429_TOO_MANY_REQUESTS(429, "Too many requests. Client error indicates that a user or application has sent too many requests to a server within a given timeframe."),
    SC_431_REQUEST_HEADER_FIELDS_TOO_LARGE(431, "Request Header Fields Too Large. The server is unwilling to process the request because its header fields are too large."),
    SC_451_UNAVAILABLE_FOR_LEGAL_REASONS(451, "Unavailable For Legal Reasons. The server is denying access to the resource as a consequence of a legal demand."),

    // 5xx Server Errors
    SC_500_INTERNAL_SERVER_ERROR(500, "Internal Server Error. Something went wrong on the server party."),
    SC_501_NOT_IMPLEMENTED(501, "Not Implemented. The server does not support the functionality required to fulfill the request."),
    SC_502_BAD_GATEWAY_ERROR(502, "Bad Gateway. Error with a downstream service."),
    SC_503_SERVICE_UNAVAILABLE(503, "Service Unavailable. The server is currently unable to handle the request due to temporary overloading or maintenance."),
    SC_504_GATEWAY_TIMEOUT(504, "Gateway Timeout. The server, while acting as a gateway or proxy, did not receive a timely response from the upstream server."),
    SC_505_HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version Not Supported. The server does not support the HTTP protocol version used in the request."),
    SC_506_VARIANT_ALSO_NEGOTIATES(506, "Variant Also Negotiates. The server has an internal configuration error: transparent content negotiation results in a circular reference."),
    SC_507_INSUFFICIENT_STORAGE(507, "Insufficient Storage. The server is unable to store the representation needed to complete the request."),
    SC_508_LOOP_DETECTED(508, "Loop Detected. The server detected an infinite loop while processing the request."),
    SC_510_NOT_EXTENDED(510, "Not Extended. Further extensions to the request are required for the server to fulfill it."),
    SC_511_NETWORK_AUTHENTICATION_REQUIRED(511, "Network Authentication Required. The client needs to authenticate to gain network access.");

    private final int code;
    private final String description;

    HttpStatusCodes(int code, String description) {
        this.code = code;
        this.description = description;
    }

}
