package pro.api4.jsonapi4j.operation.validation;

import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class CollectionValidationAssert<E>
        extends ObjectValidationAssert<CollectionValidationAssert<E>, Collection<E>> {

    CollectionValidationAssert(Collection<E> actual, ErrorSources.Source source) {
        super(actual, source);
    }

    public CollectionValidationAssert<E> isEmpty() {
        if (isSkipped()) return this;
        if (actual != null && !actual.isEmpty()) {
            fail(DefaultErrorCodes.ARRAY_LENGTH_TOO_LONG, "collection must be empty");
        }
        return this;
    }

    public CollectionValidationAssert<E> isNotEmpty() {
        if (isSkipped()) return this;
        if (actual == null || actual.isEmpty()) {
            fail(DefaultErrorCodes.ARRAY_LENGTH_TOO_SHORT, "collection can't be empty");
        }
        return this;
    }

    public CollectionValidationAssert<E> hasSize(int expected) {
        if (isSkipped()) return this;
        if (actual == null || actual.size() != expected) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                    MessageFormat.format("expected size {0}, got {1}", expected, actual == null ? "null" : actual.size()));
        }
        return this;
    }

    public CollectionValidationAssert<E> hasSizeLessThan(int max) {
        if (isSkipped()) return this;
        if (actual != null && actual.size() >= max) {
            fail(DefaultErrorCodes.ARRAY_LENGTH_TOO_LONG,
                    MessageFormat.format("size must be less than {0}", max));
        }
        return this;
    }

    public CollectionValidationAssert<E> hasSizeLessThanOrEqualTo(int max) {
        if (isSkipped()) return this;
        if (actual != null && actual.size() > max) {
            fail(DefaultErrorCodes.ARRAY_LENGTH_TOO_LONG,
                    MessageFormat.format("size can''t be more than {0}", max));
        }
        return this;
    }

    public CollectionValidationAssert<E> hasSizeGreaterThan(int min) {
        if (isSkipped()) return this;
        if (actual == null || actual.size() <= min) {
            fail(DefaultErrorCodes.ARRAY_LENGTH_TOO_SHORT,
                    MessageFormat.format("size must be greater than {0}", min));
        }
        return this;
    }

    public CollectionValidationAssert<E> hasSizeGreaterThanOrEqualTo(int min) {
        if (isSkipped()) return this;
        if (actual == null || actual.size() < min) {
            fail(DefaultErrorCodes.ARRAY_LENGTH_TOO_SHORT,
                    MessageFormat.format("size can''t be less than {0}", min));
        }
        return this;
    }

    public CollectionValidationAssert<E> hasSizeBetween(int min, int max) {
        if (isSkipped()) return this;
        if (actual == null || actual.size() < min || actual.size() > max) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                    MessageFormat.format("size must be between {0} and {1}", min, max));
        }
        return this;
    }

    public CollectionValidationAssert<E> contains(E element) {
        if (isSkipped()) return this;
        if (actual == null || !actual.contains(element)) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                    MessageFormat.format("collection must contain ''{0}''", element));
        }
        return this;
    }

    public CollectionValidationAssert<E> doesNotContain(E element) {
        if (isSkipped()) return this;
        if (actual != null && actual.contains(element)) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                    MessageFormat.format("collection must not contain ''{0}''", element));
        }
        return this;
    }

    public CollectionValidationAssert<E> containsAll(Collection<E> elements) {
        if (isSkipped()) return this;
        if (actual == null || !actual.containsAll(elements)) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR, "collection must contain all required elements");
        }
        return this;
    }

    @SafeVarargs
    public final CollectionValidationAssert<E> containsAnyOf(E... elements) {
        if (isSkipped()) return this;
        if (actual == null || Arrays.stream(elements).noneMatch(actual::contains)) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR, "collection must contain at least one of the specified elements");
        }
        return this;
    }

    public CollectionValidationAssert<E> doesNotContainNull() {
        if (isSkipped()) return this;
        if (actual != null && actual.contains(null)) {
            fail(DefaultErrorCodes.VALUE_IS_ABSENT, "collection must not contain null elements");
        }
        return this;
    }

    public CollectionValidationAssert<E> doesNotHaveDuplicates() {
        if (isSkipped()) return this;
        if (actual != null && new HashSet<>(actual).size() != actual.size()) {
            fail(DefaultErrorCodes.ARRAY_CONTAINS_DUPLICATES, "collection must not contain duplicates");
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public ObjectValidationAssert<?, List<E>> asList() {
        ObjectValidationAssert<?, List<E>> result = new ObjectValidationAssert<>((List<E>) actual, getSource());
        if (isSkipped()) result.setSkipped();
        return result;
    }

    @SuppressWarnings("unchecked")
    public ObjectValidationAssert<?, Set<E>> asSet() {
        ObjectValidationAssert<?, Set<E>> result = new ObjectValidationAssert<>((Set<E>) actual, getSource());
        if (isSkipped()) result.setSkipped();
        return result;
    }

    public CollectionValidationAssert<E> allSatisfy(Consumer<E> requirement) {
        if (isSkipped()) return this;
        if (actual != null) {
            actual.forEach(requirement);
        }
        return this;
    }

}
