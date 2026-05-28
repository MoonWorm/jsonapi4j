package pro.api4.jsonapi4j.operation.validation;

import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.request.CursorAwareRequest;
import pro.api4.jsonapi4j.request.FiltersAwareRequest;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.LimitOffsetAwareRequest;
import pro.api4.jsonapi4j.request.SortAwareRequest;
import pro.api4.jsonapi4j.request.SparseFieldsetsAwareRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Factory for building JSON:API error source objects that identify the origin of a validation error.
 *
 * <p>Provides builders for the four error source types defined by the JSON:API specification:
 * <ul>
 *   <li>{@link #pointer()} — builds a <a href="https://www.rfc-editor.org/rfc/rfc6901">JSON Pointer (RFC 6901)</a>
 *       to a value in the request document (e.g., {@code /data/attributes/title})</li>
 *   <li>{@link #parameter()} — references a query parameter (e.g., {@code filter[status]}, {@code sort})</li>
 *   <li>{@link #path()} — references a URL path segment (e.g., {@code {resourceType}}, {@code {resourceId}})</li>
 *   <li>{@link #header(String)} — references an HTTP header</li>
 * </ul>
 *
 * <p>The resulting {@link Source} objects are attached to
 * {@link pro.api4.jsonapi4j.exception.JsonApiRequestValidationException} instances and ultimately
 * serialized into the {@code source} member of a JSON:API error object.
 *
 * @see JsonApiRequestValidator
 * @see ValidationErrorCollector
 */
public class ErrorSources {

    /** Creates a builder for URL path segment error sources. */
    public static PathBuilder path() {
        return new PathBuilder();
    }

    /** Creates a header error source for the given header name. */
    public static Header header(String header) {
        return new Header(header);
    }

    /** Creates a builder for query parameter error sources. */
    public static ParameterBuilder parameter() {
        return new ParameterBuilder();
    }

    /** Creates a builder for JSON Pointer error sources. */
    public static JsonPointerBuilder pointer() {
        return new JsonPointerBuilder();
    }

    /** Builder for URL path segment sources. */
    public static class PathBuilder {

        private PathBuilder() {

        }

        /** Creates a path source for the resource type segment. */
        public Path resourceType() {
            return new Path("{resourceType}");
        }

        /** Creates a path source for the resource id segment. */
        public Path resourceId() {
            return new Path("{resourceId}");
        }

        /** Creates a path source for the relationship name segment. */
        public Path relationshipName() {
            return new Path("{relationshipName}");
        }

    }

    /** Builder for query parameter sources. */
    public static class ParameterBuilder {

        private ParameterBuilder() {

        }

        /** Creates a parameter source for the given filter name. */
        public Parameter filter(String filterName) {
            return custom(FiltersAwareRequest.getFilterParam(filterName));
        }

        /** Creates a parameter source for all filters. */
        public Parameter filters() {
            return custom(FiltersAwareRequest.getFilterParam(""));
        }

        /** Creates a parameter source for the {@code include} parameter. */
        public Parameter include() {
            return custom(IncludeAwareRequest.INCLUDE_PARAM);
        }

        /** Creates a parameter source for the {@code page[cursor]} parameter. */
        public Parameter cursor() {
            return custom(CursorAwareRequest.CURSOR_PARAM);
        }

        /** Creates a parameter source for the {@code page[limit]} parameter. */
        public Parameter limit() {
            return custom(LimitOffsetAwareRequest.LIMIT_PARAM);
        }

        /** Creates a parameter source for the {@code page[offset]} parameter. */
        public Parameter offset() {
            return custom(LimitOffsetAwareRequest.OFFSET_PARAM);
        }

        /** Creates a parameter source for the {@code sort} parameter. */
        public Parameter sort() {
            return custom(SortAwareRequest.SORT_PARAM);
        }

        /** Creates a parameter source for the {@code fields[type]} sparse fieldsets parameter. */
        public Parameter fieldSets(String resourceType) {
            return custom(SparseFieldsetsAwareRequest.getFieldsParam(resourceType));
        }

        /** Creates a parameter source for a custom query parameter. */
        public Parameter custom(String paramName) {
            return new Parameter(paramName);
        }

    }

    /**
     *  JSON Pointer [RFC6901] to the value in the request document that caused the error [e.g. "/data" for a primary data
     *  object, or "/data/attributes/title" for a specific attribute].
     */
    public static class JsonPointerBuilder extends AbstractJsonPointerBuilder {

        private JsonPointerBuilder() {
            super(new ArrayList<>());
        }

        /** Creates a JSON Pointer from custom path fragments. */
        public JsonPointer custom(String... pathFragments) {
            List<String> source = new ArrayList<>(getSource());
            source.addAll(Arrays.asList(pathFragments));
            return toPointer(source);
        }

        /** Navigates into the {@code /data} member. */
        public DataJsonPointerBuilder data() {
            List<String> source = new ArrayList<>(getSource());
            source.add("data");
            return new DataJsonPointerBuilder(source);
        }

        /** Creates a JSON Pointer to the {@code /links} member. */
        public JsonPointer links() {
            List<String> source = new ArrayList<>(getSource());
            source.add("links");
            return toPointer(source);
        }

        /** Creates a JSON Pointer to the {@code /meta} member. */
        public JsonPointer meta() {
            List<String> source = new ArrayList<>(getSource());
            source.add("meta");
            return toPointer(source);
        }

        /** Builder for JSON Pointers into the {@code data} member. */
        public static class DataJsonPointerBuilder extends AbstractJsonPointerBuilder {

            private DataJsonPointerBuilder(List<String> source) {
                super(source);
            }

            /** Navigates into the array element at the given index. */
            public DataJsonPointerBuilder index(int index) {
                List<String> source = new ArrayList<>(getSource());
                source.add(String.valueOf(index));
                return new DataJsonPointerBuilder(source);
            }

            /** Creates a JSON Pointer to the {@code id} field. */
            public JsonPointer id() {
                List<String> source = new ArrayList<>(getSource());
                source.add("id");
                return toPointer(source);
            }

            /** Creates a JSON Pointer to the {@code type} field. */
            public JsonPointer type() {
                List<String> source = new ArrayList<>(getSource());
                source.add("type");
                return toPointer(source);
            }

            /** Creates a JSON Pointer to the {@code attributes} object. */
            public JsonPointer attributes() {
                List<String> source = new ArrayList<>(getSource());
                source.add("attributes");
                return toPointer(source);
            }

            /** Creates a JSON Pointer to a specific attribute by path. */
            public JsonPointer attributes(String path) {
                List<String> source = new ArrayList<>(getSource());
                source.add("attributes");
                source.add(path);
                return toPointer(source);
            }

            /** Navigates into a relationship's data by {@link RelationshipName}. */
            public DataJsonPointerBuilder relationship(RelationshipName relationshipName) {
                return relationship(relationshipName.getName());
            }

            /** Navigates into a relationship's data by name. */
            public DataJsonPointerBuilder relationship(String relationshipName) {
                List<String> source = new ArrayList<>(getSource());
                source.add("relationships");
                source.add(relationshipName);
                source.add("data");
                return new DataJsonPointerBuilder(source);
            }

            /** Creates a JSON Pointer to the {@code links} member within data. */
            public JsonPointer links() {
                List<String> source = new ArrayList<>(getSource());
                source.add("links");
                return toPointer(source);
            }

            /** Creates a JSON Pointer to the {@code meta} member within data. */
            public JsonPointer meta() {
                List<String> source = new ArrayList<>(getSource());
                source.add("meta");
                return toPointer(source);
            }

        }

    }

    public static abstract class AbstractJsonPointerBuilder {

        public static final String FRAGMENT_SEPARATOR = "/";

        private final List<String> source;

        protected AbstractJsonPointerBuilder(List<String> source) {
            this.source = source;
        }

        protected List<String> getSource() {
            return source;
        }

        protected JsonPointer toPointer(List<String> source) {
            return new JsonPointer(FRAGMENT_SEPARATOR + String.join(FRAGMENT_SEPARATOR, source));
        }

        /** Builds the {@link JsonPointer} from the accumulated path fragments. */
        public JsonPointer toPointer() {
            return toPointer(source);
        }

    }

    /** Marker interface for all error source types. */
    public interface Source {}

    /** Error source pointing to a value in the request body via JSON Pointer. */
    public record JsonPointer(String pointer) implements Source {

        @Override
        public String toString() {
            return pointer;
        }

    }

    /** Error source pointing to a URL path segment. */
    public record Path(String path) implements Source {

        @Override
        public String toString() {
            return path;
        }

    }

    /** Error source pointing to a query parameter. */
    public record Parameter(String parameter) implements Source {

        @Override
        public String toString() {
            return parameter;
        }

    }

    /** Error source pointing to an HTTP header. */
    public record Header(String header) implements Source {

        @Override
        public String toString() {
            return header;
        }

    }

}
