package pro.api4.jsonapi4j.servlet.response.errorhandling.impl;

import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.model.document.error.ErrorCode;
import pro.api4.jsonapi4j.operation.validation.JsonApi4jConstraintViolationException;
import pro.api4.jsonapi4j.processor.exception.DataRetrievalException;
import pro.api4.jsonapi4j.processor.exception.InvalidCursorException;
import pro.api4.jsonapi4j.processor.exception.InvalidPayloadException;
import pro.api4.jsonapi4j.processor.exception.MappingException;
import pro.api4.jsonapi4j.processor.exception.ResourceNotFoundException;
import pro.api4.jsonapi4j.http.HttpStatusCodes;
import pro.api4.jsonapi4j.operation.exception.OperationNotFoundException;
import pro.api4.jsonapi4j.request.exception.BadJsonApiRequestException;
import pro.api4.jsonapi4j.request.exception.ConflictJsonApiRequestException;
import pro.api4.jsonapi4j.request.exception.ForbiddenJsonApiRequestException;
import pro.api4.jsonapi4j.servlet.response.errorhandling.ErrorHandlerFactory;
import pro.api4.jsonapi4j.servlet.response.errorhandling.ErrorsDocSupplier;
import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;
import pro.api4.jsonapi4j.servlet.response.errorhandling.ErrorsDocFactory;
import pro.api4.jsonapi4j.exception.JsonApi4jException;

import java.util.HashMap;
import java.util.Map;

public class DefaultErrorHandlerFactory implements ErrorHandlerFactory {

    public final Map<Class<? extends Throwable>, ErrorsDocSupplier<?>> errorResponseMappers;

    public DefaultErrorHandlerFactory() {
        this.errorResponseMappers = new HashMap<>();
        this.errorResponseMappers.put(JsonApi4jException.class, new ErrorsDocSupplier<JsonApi4jException>() {
            @Override
            public ErrorsDoc getErrorResponse(JsonApi4jException e) {
                return ErrorsDocFactory.genericErrorsDoc(
                        e.getHttpStatus(),
                        e.getErrorCode(),
                        e.getDetail()
                );
            }

            @Override
            public int getHttpStatus(JsonApi4jException e) {
                return e.getHttpStatus();
            }
        });
        this.errorResponseMappers.put(DataRetrievalException.class, new ErrorsDocSupplier<DataRetrievalException>() {
            @Override
            public ErrorsDoc getErrorResponse(DataRetrievalException e) {
                OperationNotFoundException operationNotFoundException = findCause(e, OperationNotFoundException.class);
                if (operationNotFoundException != null) {
                    return ErrorsDocFactory.resourceNotFoundErrorsDoc(operationNotFoundException.getMessage());
                }
                ResourceNotFoundException resourceNotFoundException = findCause(e, ResourceNotFoundException.class);
                if (resourceNotFoundException != null) {
                    return ErrorsDocFactory.resourceNotFoundErrorsDoc(resourceNotFoundException.getMessage());
                }
                InvalidCursorException invalidCursorException = findCause(e, InvalidCursorException.class);
                if (invalidCursorException != null) {
                    return ErrorsDocFactory.badRequestInvalidCursorErrorsDoc(invalidCursorException.getCursor());
                }
                InvalidPayloadException invalidPayloadException = findCause(e, InvalidPayloadException.class);
                if (invalidPayloadException != null) {
                    return ErrorsDocFactory.badRequestInvalidPayloadErrorsDoc(invalidPayloadException.getMessage());
                }
                return ErrorsDocFactory.badGatewayErrorsDoc(e.getMessage());
            }

            @Override
            public int getHttpStatus(DataRetrievalException e) {
                if (findCause(e, OperationNotFoundException.class) != null) {
                    return HttpStatusCodes.SC_404_RESOURCE_NOT_FOUND.getCode();
                }
                if (findCause(e, ResourceNotFoundException.class) != null) {
                    return HttpStatusCodes.SC_404_RESOURCE_NOT_FOUND.getCode();
                }
                if (findCause(e, InvalidCursorException.class) != null
                        || findCause(e, InvalidPayloadException.class) != null) {
                    return HttpStatusCodes.SC_400_BAD_REQUEST.getCode();
                }
                return HttpStatusCodes.SC_502_BAD_GATEWAY_ERROR.getCode();
            }
        });
        this.errorResponseMappers.put(MappingException.class, new ErrorsDocSupplier<MappingException>() {
            @Override
            public ErrorsDoc getErrorResponse(MappingException e) {
                return ErrorsDocFactory.internalServerErrorsDoc(e.getMessage());
            }

            @Override
            public int getHttpStatus(MappingException e) {
                return HttpStatusCodes.SC_500_INTERNAL_SERVER_ERROR.getCode();
            }
        });
        this.errorResponseMappers.put(ResourceNotFoundException.class, new ErrorsDocSupplier<ResourceNotFoundException>() {
            @Override
            public ErrorsDoc getErrorResponse(ResourceNotFoundException e) {
                return ErrorsDocFactory.resourceNotFoundErrorsDoc(e.getMessage());
            }

            @Override
            public int getHttpStatus(ResourceNotFoundException e) {
                return HttpStatusCodes.SC_404_RESOURCE_NOT_FOUND.getCode();
            }
        });
        this.errorResponseMappers.put(OperationNotFoundException.class, new ErrorsDocSupplier<OperationNotFoundException>() {
            @Override
            public ErrorsDoc getErrorResponse(OperationNotFoundException e) {
                return ErrorsDocFactory.resourceNotFoundErrorsDoc(e.getMessage());
            }

            @Override
            public int getHttpStatus(OperationNotFoundException e) {
                return HttpStatusCodes.SC_404_RESOURCE_NOT_FOUND.getCode();
            }
        });
        this.errorResponseMappers.put(BadJsonApiRequestException.class, new ErrorsDocSupplier<BadJsonApiRequestException>() {
            @Override
            public ErrorsDoc getErrorResponse(BadJsonApiRequestException e) {
                return ErrorsDocFactory.badRequestErrorsDoc(e.getErrorCode(), e.getMessage(), e.getParameter());
            }

            @Override
            public int getHttpStatus(BadJsonApiRequestException e) {
                return HttpStatusCodes.SC_400_BAD_REQUEST.getCode();
            }
        });
        this.errorResponseMappers.put(InvalidPayloadException.class, new ErrorsDocSupplier<InvalidPayloadException>() {
            @Override
            public ErrorsDoc getErrorResponse(InvalidPayloadException e) {
                return ErrorsDocFactory.badRequestInvalidPayloadErrorsDoc(e.getMessage());
            }

            @Override
            public int getHttpStatus(InvalidPayloadException e) {
                return HttpStatusCodes.SC_400_BAD_REQUEST.getCode();
            }
        });
        this.errorResponseMappers.put(InvalidCursorException.class, new ErrorsDocSupplier<InvalidCursorException>() {
            @Override
            public ErrorsDoc getErrorResponse(InvalidCursorException e) {
                return ErrorsDocFactory.badRequestInvalidCursorErrorsDoc(e.getCursor());
            }

            @Override
            public int getHttpStatus(InvalidCursorException e) {
                return HttpStatusCodes.SC_400_BAD_REQUEST.getCode();
            }
        });
        this.errorResponseMappers.put(ConflictJsonApiRequestException.class, new ErrorsDocSupplier<ConflictJsonApiRequestException>() {
            @Override
            public ErrorsDoc getErrorResponse(ConflictJsonApiRequestException e) {
                return ErrorsDocFactory.genericErrorsDoc(
                        HttpStatusCodes.SC_409_CONFLICT.getCode(),
                        e.getErrorCode(),
                        e.getMessage(),
                        e.getParameter()
                );
            }

            @Override
            public int getHttpStatus(ConflictJsonApiRequestException e) {
                return HttpStatusCodes.SC_409_CONFLICT.getCode();
            }
        });
        this.errorResponseMappers.put(ForbiddenJsonApiRequestException.class, new ErrorsDocSupplier<ForbiddenJsonApiRequestException>() {
            @Override
            public ErrorsDoc getErrorResponse(ForbiddenJsonApiRequestException e) {
                return ErrorsDocFactory.genericErrorsDoc(
                        HttpStatusCodes.SC_403_FORBIDDEN.getCode(),
                        e.getErrorCode(),
                        e.getMessage(),
                        e.getParameter()
                );
            }

            @Override
            public int getHttpStatus(ForbiddenJsonApiRequestException e) {
                return HttpStatusCodes.SC_403_FORBIDDEN.getCode();
            }
        });
        this.errorResponseMappers.put(JsonApi4jConstraintViolationException.class, new ErrorsDocSupplier<JsonApi4jConstraintViolationException>() {
            @Override
            public ErrorsDoc getErrorResponse(JsonApi4jConstraintViolationException e) {
                return ErrorsDocFactory.badRequestErrorsDoc(
                        DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                        e.getDetail(),
                        e.getParameter()
                );
            }

            @Override
            public int getHttpStatus(JsonApi4jConstraintViolationException e) {
                return HttpStatusCodes.SC_400_BAD_REQUEST.getCode();
            }
        });
    }

    @Override
    public Map<Class<? extends Throwable>, ErrorsDocSupplier<?>> getErrorResponseMappers() {
        return this.errorResponseMappers;
    }

    private static <T extends Throwable> T findCause(Throwable throwable,
                                                     Class<T> targetType) {
        Throwable current = throwable;
        while (current != null) {
            if (targetType.isInstance(current)) {
                return targetType.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }

}
