package io.jsonapi4j.servlet.response.errorhandling.impl;

import io.jsonapi4j.processor.exception.DataRetrievalException;
import io.jsonapi4j.processor.exception.MappingException;
import io.jsonapi4j.processor.exception.ResourceNotFoundException;
import io.jsonapi4j.http.HttpStatusCodes;
import io.jsonapi4j.operation.exception.OperationNotFoundException;
import io.jsonapi4j.plugin.ac.exception.AccessControlMisconfigurationException;
import io.jsonapi4j.request.exception.BadJsonApiRequestException;
import io.jsonapi4j.servlet.response.errorhandling.ErrorHandlerFactory;
import io.jsonapi4j.servlet.response.errorhandling.ErrorsDocSupplier;
import io.jsonapi4j.model.document.error.ErrorsDoc;
import io.jsonapi4j.servlet.response.errorhandling.ErrorsDocFactory;
import io.jsonapi4j.exception.JsonApi4jException;

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
        this.errorResponseMappers.put(AccessControlMisconfigurationException.class, new ErrorsDocSupplier<AccessControlMisconfigurationException>() {
            @Override
            public ErrorsDoc getErrorResponse(AccessControlMisconfigurationException e) {
                return ErrorsDocFactory.internalServerErrorsDoc();
            }

            @Override
            public int getHttpStatus(AccessControlMisconfigurationException e) {
                return HttpStatusCodes.SC_500_INTERNAL_SERVER_ERROR.getCode();
            }
        });
    }

    @Override
    public Map<Class<? extends Throwable>, ErrorsDocSupplier<?>> getErrorResponseMappers() {
        return this.errorResponseMappers;
    }

}
