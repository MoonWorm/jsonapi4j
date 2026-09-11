package pro.api4.jsonapi4j.sampleapp.oas;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasCustomizer;

import java.util.List;
import java.util.Objects;

/**
 * Documents the rate-limiting headers the gateway in front of this app adds to every {@code 429}.
 * <p>
 * The framework knows nothing about them, which is the point: customizers run after the built-in ones and see the
 * finished document, so anything the generator produced can be tuned or extended from the application.
 */
public class RateLimitHeadersCustomizer implements OasCustomizer {

    private static final String TOO_MANY_REQUESTS = "429";

    private static final List<RateLimitHeader> HEADERS = List.of(
            new RateLimitHeader(
                    "X-RateLimit-Remaining",
                    "Number of tokens currently remaining. Refer to X-RateLimit-Replenish-Rate header for replenishment information.",
                    5
            ),
            new RateLimitHeader(
                    "X-RateLimit-Burst-Capacity",
                    "Initial number of tokens, and max number of tokens that can be replenished.",
                    20
            ),
            new RateLimitHeader(
                    "X-RateLimit-Replenish-Rate",
                    "Number of tokens replenished per second.",
                    5
            ),
            new RateLimitHeader(
                    "X-RateLimit-Requested-Tokens",
                    "Request cost in tokens.",
                    5
            )
    );

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            return;
        }
        openApi.getPaths().values().stream()
                .flatMap(pathItem -> pathItem.readOperations().stream())
                .map(operation -> operation.getResponses().get(TOO_MANY_REQUESTS))
                .filter(Objects::nonNull)
                .forEach(this::addRateLimitHeaders);
    }

    private void addRateLimitHeaders(ApiResponse response) {
        HEADERS.forEach(header -> response.addHeaderObject(header.name(), new Header()
                .required(true)
                .description(header.description())
                .schema(new IntegerSchema())
                .example(header.example())));
    }

    private record RateLimitHeader(String name, String description, int example) {
    }

}
