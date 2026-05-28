package pro.api4.jsonapi4j.operation.validation;

import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Fluent assertion class for validating {@link Collection} values in JSON:API requests.
 *
 * <p>Extends {@link ObjectValidationAssert} with collection-specific constraints: empty/non-empty checks,
 * size bounds, element membership, duplicate detection, null element checks, and per-element validation
 * via {@link #allSatisfy(Consumer)}.
 *
 * <p>Usage example:
 * {@snippet :
 *   Validate.assertThat(request.getOriginalIncludes())
 *           .isNotEmpty()
 *           .hasSizeLessThanOrEqualTo(10)
 *           .doesNotHaveDuplicates();
 * }
 *
 * @param <E> the element type of the collection
 * @see Validate
 * @see ObjectValidationAssert
 */
public class CollectionValidationAssert<E>
        extends ObjectValidationAssert<CollectionValidationAssert<E>, Collection<E>> {

    CollectionValidationAssert(Collection<E> actual, ErrorSources.Source source) {
        super(actual, source);
    }

    /** Asserts the collection is empty. */
    public CollectionValidationAssert<E> isEmpty() {
        if (isSkipped()) return this;
        if (actual != null && !actual.isEmpty()) {
            fail(DefaultErrorCodes.ARRAY_LENGTH_TOO_LONG, "collection must be empty");
        }
        return this;
    }

    /** Asserts the collection is not empty. */
    public CollectionValidationAssert<E> isNotEmpty() {
        if (isSkipped()) return this;
        if (actual == null || actual.isEmpty()) {
            fail(DefaultErrorCodes.ARRAY_LENGTH_TOO_SHORT, "collection can't be empty");
        }
        return this;
    }

    /** Asserts the collection has exactly the given size. */
    public CollectionValidationAssert<E> hasSize(int expected) {
        if (isSkipped()) return this;
        if (actual == null || actual.size() != expected) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                    MessageFormat.format("expected size {0}, got {1}", expected, actual == null ? "null" : actual.size()));
        }
        return this;
    }

    /** Asserts the collection size is strictly less than {@code max}. */
    public CollectionValidationAssert<E> hasSizeLessThan(int max) {
        if (isSkipped()) return this;
        if (actual != null && actual.size() >= max) {
            fail(DefaultErrorCodes.ARRAY_LENGTH_TOO_LONG,
                    MessageFormat.format("size must be less than {0}", max));
        }
        return this;
    }

    /** Asserts the collection size is at most {@code max}. */
    public CollectionValidationAssert<E> hasSizeLessThanOrEqualTo(int max) {
        if (isSkipped()) return this;
        if (actual != null && actual.size() > max) {
            fail(DefaultErrorCodes.ARRAY_LENGTH_TOO_LONG,
                    MessageFormat.format("size can''t be more than {0}", max));
        }
        return this;
    }

    /** Asserts the collection size is strictly greater than {@code min}. */
    public CollectionValidationAssert<E> hasSizeGreaterThan(int min) {
        if (isSkipped()) return this;
        if (actual == null || actual.size() <= min) {
            fail(DefaultErrorCodes.ARRAY_LENGTH_TOO_SHORT,
                    MessageFormat.format("size must be greater than {0}", min));
        }
        return this;
    }

    /** Asserts the collection size is at least {@code min}. */
    public CollectionValidationAssert<E> hasSizeGreaterThanOrEqualTo(int min) {
        if (isSkipped()) return this;
        if (actual == null || actual.size() < min) {
            fail(DefaultErrorCodes.ARRAY_LENGTH_TOO_SHORT,
                    MessageFormat.format("size can''t be less than {0}", min));
        }
        return this;
    }

    /** Asserts the collection size is between {@code min} and {@code max} (inclusive). */
    public CollectionValidationAssert<E> hasSizeBetween(int min, int max) {
        if (isSkipped()) return this;
        if (actual == null || actual.size() < min || actual.size() > max) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                    MessageFormat.format("size must be between {0} and {1}", min, max));
        }
        return this;
    }

    /** Asserts the collection contains the given element. */
    public CollectionValidationAssert<E> contains(E element) {
        if (isSkipped()) return this;
        if (actual == null || !actual.contains(element)) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                    MessageFormat.format("collection must contain ''{0}''", element));
        }
        return this;
    }

    /** Asserts the collection does not contain the given element. */
    public CollectionValidationAssert<E> doesNotContain(E element) {
        if (isSkipped()) return this;
        if (actual != null && actual.contains(element)) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                    MessageFormat.format("collection must not contain ''{0}''", element));
        }
        return this;
    }

    /** Asserts the collection contains all elements from the given collection. */
    public CollectionValidationAssert<E> containsAll(Collection<E> elements) {
        if (isSkipped()) return this;
        if (actual == null || !actual.containsAll(elements)) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR, "collection must contain all required elements");
        }
        return this;
    }

    /** Asserts the collection contains at least one of the given elements. */
    @SafeVarargs
    public final CollectionValidationAssert<E> containsAnyOf(E... elements) {
        if (isSkipped()) return this;
        if (actual == null || Arrays.stream(elements).noneMatch(actual::contains)) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR, "collection must contain at least one of the specified elements");
        }
        return this;
    }

    /** Asserts the collection does not contain any null elements. */
    public CollectionValidationAssert<E> doesNotContainNull() {
        if (isSkipped()) return this;
        if (actual != null && actual.contains(null)) {
            fail(DefaultErrorCodes.VALUE_IS_ABSENT, "collection must not contain null elements");
        }
        return this;
    }

    /** Asserts the collection contains no duplicate elements. */
    public CollectionValidationAssert<E> doesNotHaveDuplicates() {
        if (isSkipped()) return this;
        if (actual != null && new HashSet<>(actual).size() != actual.size()) {
            fail(DefaultErrorCodes.ARRAY_CONTAINS_DUPLICATES, "collection must not contain duplicates");
        }
        return this;
    }

    /** Narrows this assertion to a {@link List}-typed {@link ObjectValidationAssert}. */
    public ObjectValidationAssert<?, List<E>> asList() {
        ObjectValidationAssert<?, List<E>> result = new ObjectValidationAssert<>((List<E>) actual, getSource());
        if (isSkipped()) result.setSkipped();
        return result;
    }

    /** Narrows this assertion to a {@link Set}-typed {@link ObjectValidationAssert}. */
    public ObjectValidationAssert<?, Set<E>> asSet() {
        ObjectValidationAssert<?, Set<E>> result = new ObjectValidationAssert<>((Set<E>) actual, getSource());
        if (isSkipped()) result.setSkipped();
        return result;
    }

    /** Asserts that all elements in the collection satisfy the given requirement. */
    public CollectionValidationAssert<E> allSatisfy(Consumer<E> requirement) {
        if (isSkipped()) return this;
        if (actual != null) {
            actual.forEach(requirement);
        }
        return this;
    }

}
