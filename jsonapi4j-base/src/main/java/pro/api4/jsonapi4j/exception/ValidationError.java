package pro.api4.jsonapi4j.exception;

import pro.api4.jsonapi4j.model.document.error.ErrorCode;
import pro.api4.jsonapi4j.operation.validation.ErrorSources;

/**
 * Immutable record representing a single validation error.
 * <p>
 * Aggregated into a {@link CompositeJsonApiRequestValidationException} when multiple validation
 * errors are collected in one pass. The framework maps each instance to a JSON:API error object
 * with the appropriate {@code "code"}, {@code "detail"}, and {@code "source"} members.
 *
 * @param errorCode the machine-readable error code
 * @param detail    a human-readable description of the validation failure
 * @param source    the request location (JSON Pointer or query parameter) that caused the error;
 *                  {@code null} if the error is not attributable to a specific location
 */
public record ValidationError(ErrorCode errorCode, String detail, ErrorSources.Source source) {

}
