package pro.api4.jsonapi4j.plugin.cd;

import pro.api4.jsonapi4j.compound.docs.exception.DomainResolutionException;
import pro.api4.jsonapi4j.compound.docs.exception.DownstreamTimeoutException;
import pro.api4.jsonapi4j.compound.docs.exception.ErrorJsonApiResponseException;
import pro.api4.jsonapi4j.errorhandling.ErrorHandlerFactory;
import pro.api4.jsonapi4j.errorhandling.ErrorsDocFactory;
import pro.api4.jsonapi4j.errorhandling.ErrorsDocSupplier;
import pro.api4.jsonapi4j.model.document.error.ApiGatewayErrorCodes;
import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;

import java.util.Map;
import java.util.function.Supplier;

import static pro.api4.jsonapi4j.http.HttpStatusCodes.SC_500_INTERNAL_SERVER_ERROR;
import static pro.api4.jsonapi4j.http.HttpStatusCodes.SC_502_BAD_GATEWAY_ERROR;
import static pro.api4.jsonapi4j.http.HttpStatusCodes.SC_504_GATEWAY_TIMEOUT;

/**
 * JSON:API error responses for include resolution failing under
 * {@link pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy#FAIL}: {@code 504} for a downstream timeout,
 * {@code 502} for any other downstream failure, and {@code 500} for a resource type with no route, which is a
 * configuration problem. Details never name downstream URLs, which are internal.
 *
 * <p>Contributed through {@link JsonApiCompoundDocsPlugin}, so an application that maps these exceptions itself keeps
 * its own mapping.
 */
public class CompoundDocsErrorHandlerFactory implements ErrorHandlerFactory {

    @Override
    public Map<Class<? extends Throwable>, ErrorsDocSupplier<?>> getErrorResponseMappers() {
        return Map.of(
                DownstreamTimeoutException.class, mapper(
                        SC_504_GATEWAY_TIMEOUT.getCode(),
                        () -> ErrorsDocFactory.genericErrorsDoc(
                                SC_504_GATEWAY_TIMEOUT.getCode(),
                                ApiGatewayErrorCodes.GATEWAY_TIMEOUT,
                                "Timed out resolving included resources"
                        )
                ),
                ErrorJsonApiResponseException.class, mapper(
                        SC_502_BAD_GATEWAY_ERROR.getCode(),
                        () -> ErrorsDocFactory.badGatewayErrorsDoc("Failed to resolve included resources")
                ),
                DomainResolutionException.class, mapper(
                        SC_500_INTERNAL_SERVER_ERROR.getCode(),
                        ErrorsDocFactory::internalServerErrorsDoc
                )
        );
    }

    /**
     * @param errorsDoc builds the document per response, as each error object gets its own id
     */
    private static ErrorsDocSupplier<Throwable> mapper(int status, Supplier<ErrorsDoc> errorsDoc) {
        return new ErrorsDocSupplier<>() {
            @Override
            public ErrorsDoc getErrorResponse(Throwable e) {
                return errorsDoc.get();
            }

            @Override
            public int getHttpStatus(Throwable e) {
                return status;
            }
        };
    }

}
