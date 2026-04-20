package pro.api4.jsonapi4j.servlet.response.errorhandling.impl;

import pro.api4.jsonapi4j.exception.ConstraintViolationException;
import pro.api4.jsonapi4j.processor.exception.DataRetrievalException;
import pro.api4.jsonapi4j.processor.exception.MappingException;
import pro.api4.jsonapi4j.http.HttpStatusCodes;
import pro.api4.jsonapi4j.operation.exception.OperationNotFoundException;
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
        this.errorResponseMappers.put(DataRetrievalException.class, new ErrorsDocSupplier<DataRetrievalException>() {
            @Override
            public ErrorsDoc getErrorResponse(DataRetrievalException e) {
                return ErrorsDocFactory.badGatewayErrorsDoc(e.getMessage());
            }

            @Override
            public int getHttpStatus(DataRetrievalException e) {
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
        this.errorResponseMappers.put(ConstraintViolationException.class, new ErrorsDocSupplier<ConstraintViolationException>() {
            @Override
            public ErrorsDoc getErrorResponse(ConstraintViolationException e) {
                return ErrorsDocFactory.badRequestErrorsDoc(e.getErrorCode(), e.getDetail(), e.getParameter());
            }

            @Override
            public int getHttpStatus(ConstraintViolationException e) {
                return HttpStatusCodes.SC_400_BAD_REQUEST.getCode();
            }
        });
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
    }

    @Override
    public Map<Class<? extends Throwable>, ErrorsDocSupplier<?>> getErrorResponseMappers() {
        return this.errorResponseMappers;
    }

}
