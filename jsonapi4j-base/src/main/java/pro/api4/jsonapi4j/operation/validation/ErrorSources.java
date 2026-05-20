package pro.api4.jsonapi4j.operation.validation;

import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.request.CursorAwareRequest;
import pro.api4.jsonapi4j.request.FiltersAwareRequest;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.LimitOffsetAwareRequest;
import pro.api4.jsonapi4j.request.SortAwareRequest;

import java.util.ArrayList;
import java.util.List;

public class ErrorSources {

    public static final String FRAGMENT_SEPARATOR = " -> ";

    public static UrlSources url() {
        return new UrlSources(List.of("$url"));
    }

    public static PayloadSources payload() {
        return new PayloadSources(List.of("$body"));
    }

    public static abstract class SourceBuilder {

        private final List<String> source;

        public SourceBuilder(List<String> source) {
            this.source = source;
        }

        public List<String> getSource() {
            return source;
        }

        protected ParameterPath toParameter(List<String> source) {
            return new ParameterPath(String.join(FRAGMENT_SEPARATOR, source));
        }

        public ParameterPath toParameter() {
            return toParameter(source);
        }

    }

    public static class UrlSources extends SourceBuilder {

        public UrlSources(List<String> source) {
            super(source);
        }

        public PathSources path() {
            List<String> source = new ArrayList<>(getSource());
            source.add("$path");
            return new PathSources(source);
        }

        public QueryParamSources queryParams() {
            List<String> source = new ArrayList<>(getSource());
            source.add("$queryParams");
            return new QueryParamSources(source);
        }

        public static class PathSources extends SourceBuilder {

            public PathSources(List<String> source) {
                super(source);
            }

            public ParameterPath resourceType() {
                List<String> source = new ArrayList<>(getSource());
                source.add("{resourceType}");
                return toParameter(source);
            }

            public ParameterPath resourceId() {
                List<String> source = new ArrayList<>(getSource());
                source.add("{resourceId}");
                return toParameter(source);
            }

            public ParameterPath relationshipName() {
                List<String> source = new ArrayList<>(getSource());
                source.add("{relationshipName}");
                return toParameter(source);
            }

        }

        public static class QueryParamSources extends SourceBuilder {

            public QueryParamSources(List<String> source) {
                super(source);
            }

            public ParameterPath filter(String filterName) {
                return custom(FiltersAwareRequest.getFilterParam(filterName));
            }

            public ParameterPath filters() {
                return custom(FiltersAwareRequest.getFilterParam(""));
            }

            public ParameterPath include() {
                return custom(IncludeAwareRequest.INCLUDE_PARAM);
            }

            public ParameterPath cursor() {
                return custom(CursorAwareRequest.CURSOR_PARAM);
            }

            public ParameterPath limit() {
                return custom(LimitOffsetAwareRequest.LIMIT_PARAM);
            }

            public ParameterPath offset() {
                return custom(LimitOffsetAwareRequest.OFFSET_PARAM);
            }

            public ParameterPath sort() {
                return custom(SortAwareRequest.SORT_PARAM);
            }

            public ParameterPath custom(String paramName) {
                List<String> source = new ArrayList<>(getSource());
                source.add(paramName);
                return toParameter(source);
            }

        }

    }

    public static class PayloadSources extends SourceBuilder {

        public PayloadSources(List<String> source) {
            super(source);
        }

        public PayloadDataSources data() {
            List<String> source = new ArrayList<>(getSource());
            source.add("data");
            return new PayloadDataSources(source);
        }

        public ParameterPath links() {
            List<String> source = new ArrayList<>(getSource());
            source.add("links");
            return toParameter(source);
        }

        public ParameterPath meta() {
            List<String> source = new ArrayList<>(getSource());
            source.add("meta");
            return toParameter(source);
        }

        public static class PayloadDataSources extends SourceBuilder {

            public PayloadDataSources(List<String> source) {
                super(source);
            }

            public PayloadDataSources index(int index) {
                List<String> source = new ArrayList<>(getSource());
                if (!source.isEmpty() && source.getLast().equals("data")) {
                    source.set(source.size() - 1, "data[" + index + "]");
                }
                return new PayloadDataSources(source);
            }

            public ParameterPath id() {
                List<String> source = new ArrayList<>(getSource());
                source.add("id");
                return toParameter(source);
            }

            public ParameterPath type() {
                List<String> source = new ArrayList<>(getSource());
                source.add("type");
                return toParameter(source);
            }

            public ParameterPath attributes() {
                List<String> source = new ArrayList<>(getSource());
                source.add("attributes");
                return toParameter(source);
            }

            public ParameterPath attributes(String path) {
                List<String> source = new ArrayList<>(getSource());
                source.add("attributes");
                source.add(path);
                return toParameter(source);
            }

            public PayloadDataSources relationship(RelationshipName relationshipName) {
                return relationship(relationshipName.getName());
            }

            public PayloadDataSources relationship(String relationshipName) {
                List<String> source = new ArrayList<>(getSource());
                source.add("relationships");
                source.add(relationshipName);
                source.add("data");
                return new PayloadDataSources(source);
            }

            public ParameterPath links() {
                List<String> source = new ArrayList<>(getSource());
                source.add("links");
                return toParameter(source);
            }

            public ParameterPath meta() {
                List<String> source = new ArrayList<>(getSource());
                source.add("meta");
                return toParameter(source);
            }

        }

    }

    public record ParameterPath(String path) {
    }

}
