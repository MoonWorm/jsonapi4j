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

public class ErrorSources {

    public static PathBuilder path() {
        return new PathBuilder();
    }

    public static Header header(String header) {
        return new Header(header);
    }

    public static ParameterBuilder parameter() {
        return new ParameterBuilder();
    }

    public static JsonPointerBuilder pointer() {
        return new JsonPointerBuilder();
    }

    public static class PathBuilder {

        private PathBuilder() {

        }

        public Path resourceType() {
            return new Path("{resourceType}");
        }

        public Path resourceId() {
            return new Path("{resourceId}");
        }

        public Path relationshipName() {
            return new Path("{relationshipName}");
        }

    }

    public static class ParameterBuilder {

        private ParameterBuilder() {

        }

        public Parameter filter(String filterName) {
            return custom(FiltersAwareRequest.getFilterParam(filterName));
        }

        public Parameter filters() {
            return custom(FiltersAwareRequest.getFilterParam(""));
        }

        public Parameter include() {
            return custom(IncludeAwareRequest.INCLUDE_PARAM);
        }

        public Parameter cursor() {
            return custom(CursorAwareRequest.CURSOR_PARAM);
        }

        public Parameter limit() {
            return custom(LimitOffsetAwareRequest.LIMIT_PARAM);
        }

        public Parameter offset() {
            return custom(LimitOffsetAwareRequest.OFFSET_PARAM);
        }

        public Parameter sort() {
            return custom(SortAwareRequest.SORT_PARAM);
        }

        public Parameter fieldSets(String resourceType) {
            return custom(SparseFieldsetsAwareRequest.getFieldsParam(resourceType));
        }

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

        public JsonPointer custom(String... pathFragments) {
            List<String> source = new ArrayList<>(getSource());
            source.addAll(Arrays.asList(pathFragments));
            return toPointer(source);
        }

        public DataJsonPointerBuilder data() {
            List<String> source = new ArrayList<>(getSource());
            source.add("data");
            return new DataJsonPointerBuilder(source);
        }

        public JsonPointer links() {
            List<String> source = new ArrayList<>(getSource());
            source.add("links");
            return toPointer(source);
        }

        public JsonPointer meta() {
            List<String> source = new ArrayList<>(getSource());
            source.add("meta");
            return toPointer(source);
        }

        public static class DataJsonPointerBuilder extends AbstractJsonPointerBuilder {

            private DataJsonPointerBuilder(List<String> source) {
                super(source);
            }

            public DataJsonPointerBuilder index(int index) {
                List<String> source = new ArrayList<>(getSource());
                source.add(String.valueOf(index));
                return new DataJsonPointerBuilder(source);
            }

            public JsonPointer id() {
                List<String> source = new ArrayList<>(getSource());
                source.add("id");
                return toPointer(source);
            }

            public JsonPointer type() {
                List<String> source = new ArrayList<>(getSource());
                source.add("type");
                return toPointer(source);
            }

            public JsonPointer attributes() {
                List<String> source = new ArrayList<>(getSource());
                source.add("attributes");
                return toPointer(source);
            }

            public JsonPointer attributes(String path) {
                List<String> source = new ArrayList<>(getSource());
                source.add("attributes");
                source.add(path);
                return toPointer(source);
            }

            public DataJsonPointerBuilder relationship(RelationshipName relationshipName) {
                return relationship(relationshipName.getName());
            }

            public DataJsonPointerBuilder relationship(String relationshipName) {
                List<String> source = new ArrayList<>(getSource());
                source.add("relationships");
                source.add(relationshipName);
                source.add("data");
                return new DataJsonPointerBuilder(source);
            }

            public JsonPointer links() {
                List<String> source = new ArrayList<>(getSource());
                source.add("links");
                return toPointer(source);
            }

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

        public JsonPointer toPointer() {
            return toPointer(source);
        }

    }

    public interface Source {}

    // body
    public record JsonPointer(String pointer) implements Source {

        @Override
        public String toString() {
            return pointer;
        }

    }

    // path
    public record Path(String path) implements Source {

        @Override
        public String toString() {
            return path;
        }

    }

    // query param
    public record Parameter(String parameter) implements Source {

        @Override
        public String toString() {
            return parameter;
        }

    }

    // header
    public record Header(String header) implements Source {

        @Override
        public String toString() {
            return header;
        }

    }

}
