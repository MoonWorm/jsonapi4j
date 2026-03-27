package pro.api4.jsonapi4j.compound.docs;

import java.util.List;
import java.util.Map;

public record DefaultCompoundDocsRequest(
        String method,
        List<String> includes,
        Map<String, List<String>> fieldSets,
        Map<String, String> headers,
        String relativePath,
        Map<String, List<String>> customQueryParams
) implements CompoundDocsRequest {
}
