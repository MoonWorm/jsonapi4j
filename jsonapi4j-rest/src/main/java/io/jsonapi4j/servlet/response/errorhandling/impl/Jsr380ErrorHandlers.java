package io.jsonapi4j.servlet.response.errorhandling.impl;

import io.jsonapi4j.http.HttpStatusCodes;
import io.jsonapi4j.model.document.error.DefaultErrorCodes;
import io.jsonapi4j.model.document.error.ErrorCode;
import io.jsonapi4j.model.document.error.ErrorObject;
import io.jsonapi4j.model.document.error.ErrorsDoc;
import io.jsonapi4j.servlet.response.errorhandling.ErrorHandlerFactory;
import io.jsonapi4j.servlet.response.errorhandling.ErrorsDocFactory;
import io.jsonapi4j.servlet.response.errorhandling.ErrorsDocSupplier;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Annotation;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

public final class Jsr380ErrorHandlers implements ErrorHandlerFactory {

    private final Jsr380ErrorHandlerHelper jsr380ErrorHandlerHelper = new Jsr380ErrorHandlerHelper();

    private final Map<Class<? extends Throwable>, ErrorsDocSupplier<?>> errorResponseMappers;

    public Jsr380ErrorHandlers() {
        this.errorResponseMappers = new HashMap<>();
        this.errorResponseMappers.put(
                ConstraintViolationException.class,
                new ErrorsDocSupplier<ConstraintViolationException>() {
                    @Override
                    public ErrorsDoc getErrorResponse(ConstraintViolationException e) {
                        return jsr380ErrorHandlerHelper.handleConstraintViolationException(e);
                    }

                    @Override
                    public int getHttpStatus(ConstraintViolationException e) {
                        return HttpStatusCodes.SC_400_BAD_REQUEST.getCode();
                    }
                }
        );
    }

    @Override
    public Map<Class<? extends Throwable>, ErrorsDocSupplier<?>> getErrorResponseMappers() {
        return this.errorResponseMappers;
    }

    public static class Jsr380ErrorHandlerHelper {

        private final PropertyPathResolver pathResolver = new HibernateValidatorPropertyPathResolver();

        public ErrorsDoc handleConstraintViolationException(ConstraintViolationException constraintViolationException) {
            List<ErrorObject> errors = constraintViolationException.getConstraintViolations().stream()
                    .map(this::toError)
                    .sorted(Comparator.comparing(ErrorObject::getCode).thenComparing(ErrorObject::getDetail))
                    .toList();
            return new ErrorsDoc(errors);
        }

        ErrorObject toError(ConstraintViolation<?> cv) {
            return ErrorsDocFactory.errorObject(
                    HttpStatusCodes.SC_400_BAD_REQUEST.getCode(),
                    resolveCode(cv),
                    cv.getMessage(),
                    pathResolver.resolveFieldName(cv.getPropertyPath())
            );
        }

        ErrorCode resolveCode(ConstraintViolation<?> constraintViolation) {
            Object invalidValue = constraintViolation.getInvalidValue();
            Annotation constraintAnnotation = constraintViolation
                    .getConstraintDescriptor()
                    .getAnnotation();
            if (constraintAnnotation instanceof NotNull) {
                return DefaultErrorCodes.VALUE_IS_ABSENT;
            } else if (constraintAnnotation instanceof NotBlank) {
                return DefaultErrorCodes.VALUE_EMPTY;
            } else if (constraintAnnotation instanceof Size) {
                if (invalidValue instanceof Iterable) {
                    return DefaultErrorCodes.ARRAY_LENGTH_TOO_LONG;
                } else {
                    return DefaultErrorCodes.VALUE_TOO_LONG;
                }
            } else if (constraintAnnotation instanceof Pattern
                    || constraintAnnotation instanceof Digits) {
                return DefaultErrorCodes.VALUE_INVALID_FORMAT;
            } else if (constraintAnnotation instanceof Positive) {
                return DefaultErrorCodes.VALUE_TOO_LOW;
            } else if (constraintAnnotation instanceof Max) {
                return DefaultErrorCodes.VALUE_TOO_HIGH;
            } else {
                return DefaultErrorCodes.GENERIC_REQUEST_ERROR;
            }
        }

        public interface PropertyPathResolver {

            String resolveFieldName(Path path);

        }

        /**
         * This class helps to extract human-readable error path name from the full property path.
         * The logic is based on the behaviour of the Hibernate validation framework and the way how it generates paths
         * for the {@link jakarta.validation.Path} implementation.
         * <p>
         * Examples:
         * <ul>
         *     <li>countryRegion -> countryRegion</li>
         *     <li>validateCountryRegion.countryRegion -> countryRegion</li>
         *     <li>validateFilterByIds.resourceIds[1].&#10216list element&#10217 -> resourceIds[1]</li>
         * </ul>
         */
        public static class HibernateValidatorPropertyPathResolver implements PropertyPathResolver {

            private static final java.util.regex.Pattern COMPLEX_PATH_TO_ARRAY_REGEXP = java.util.regex.Pattern.compile("(.*\\.(.*\\[\\d+\\]).*)|((.*\\[\\d+\\]).*)");

            @Override
            public String resolveFieldName(Path path) {
                if (path == null) {
                    return null;
                }
                String pathStr = path.toString();
                if (isComplexPath(pathStr)) {
                    if (isComplexPathToArray(pathStr)) {
                        return extractArrayPropertySubpath(pathStr);
                    } else {
                        return extractRegularPropertySubpath(pathStr);
                    }
                }
                return pathStr;
            }

            private String extractArrayPropertySubpath(String path) {
                Matcher matcher = COMPLEX_PATH_TO_ARRAY_REGEXP.matcher(path);
                while (matcher.find()) {
                    for (int i = 0; i <= matcher.groupCount(); i++) {
                        String group = matcher.group(i);
                        if (group.contains("[") && group.contains("]") && !group.contains(".")) {
                            return group;
                        }
                    }
                }
                return path;
            }

            private String extractRegularPropertySubpath(String path) {
                return path.substring(path.lastIndexOf(".") + 1);
            }

            private boolean isComplexPathToArray(String path) {
                Matcher matcher = COMPLEX_PATH_TO_ARRAY_REGEXP.matcher(path);
                return matcher.matches();
            }

            private boolean isComplexPath(String path) {
                return path.contains(".");
            }

        }

    }
}
