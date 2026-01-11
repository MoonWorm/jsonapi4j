package pro.api4.jsonapi4j.operation.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

public class JsonApi4jDefaultValidator {

    public void validateNonNull(Object object) {
        NonNullObject nonNullObject = new NonNullObject(object);
        validateInternal(nonNullObject);
    }

    public void validateFilterByIds(List<String> resourceIds) {
        JsonApiIds jsonApiIds = new JsonApiIds(resourceIds.stream().map(JsonApiId::new).toList());
        validateInternal(jsonApiIds);
    }

    public void validateResourceId(String resourceId) {
        validateInternal(new JsonApiId(resourceId));
    }

    private <T> void validateInternal(T data) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            Set<ConstraintViolation<T>> violations = validator.validate(data);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException("Default JSON:API request validation failed", violations);
            }
        }
    }

    public record NonNullObject(@NotNull Object object) {}

    public record JsonApiIds(@Size(max = 20) List<@Valid JsonApiId> ids) {}

    public record JsonApiId(@NotBlank @Size(min = 1, max = 64) String id) {}

}
