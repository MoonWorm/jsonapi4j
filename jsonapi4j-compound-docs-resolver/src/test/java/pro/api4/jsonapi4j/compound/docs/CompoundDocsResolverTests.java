package pro.api4.jsonapi4j.compound.docs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pro.api4.jsonapi4j.compound.docs.client.BatchFetchResult;
import pro.api4.jsonapi4j.compound.docs.client.CachingCompoundDocsFetcher;
import pro.api4.jsonapi4j.compound.docs.config.CompoundDocsResolverConfig;
import pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy;
import pro.api4.jsonapi4j.processor.IdAndType;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static pro.api4.jsonapi4j.compound.docs.IdAndTypeFixtures.idAndType;

@ExtendWith(MockitoExtension.class)
public class CompoundDocsResolverTests {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final IdAndType USER_1 = idAndType("users", "1");
    private static final IdAndType USER_2 = idAndType("users", "2");
    private static final IdAndType USER_3 = idAndType("users", "3");
    private static final IdAndType NORWAY = idAndType("countries", "NO");
    private static final IdAndType FINLAND = idAndType("countries", "FI");
    private static final IdAndType USA = idAndType("countries", "US");
    private static final IdAndType NOK = idAndType("currencies", "NOK");
    private static final IdAndType EUR = idAndType("currencies", "EUR");
    private static final IdAndType USD = idAndType("currencies", "USD");

    private static final Map<IdAndType, Map<String, List<IdAndType>>> DOWNSTREAM = Map.of(
            USER_1, Map.of(
                    "relatives", List.of(USER_2, USER_3),
                    "citizenships", List.of(NORWAY, USA),
                    "placeOfBirth", List.of(USA)
            ),
            USER_2, Map.of(
                    "relatives", List.of(USER_3),
                    "placeOfBirth", List.of(FINLAND)
            ),
            USER_3, Map.of(
                    "relatives", List.of(),
                    "placeOfBirth", List.of(NORWAY)
            ),
            NORWAY, Map.of("currencies", List.of(NOK)),
            FINLAND, Map.of("currencies", List.of(EUR)),
            USA, Map.of("currencies", List.of(USD))
    );

    private record FetchCall(String type, Set<String> ids, Set<String> includes) {
    }

    @Mock
    private CachingCompoundDocsFetcher fetcher;

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private final List<FetchCall> fetchCalls = Collections.synchronizedList(new ArrayList<>());

    @AfterEach
    public void tearDown() {
        executorService.shutdownNow();
    }

    @Nested
    class ResolveCompoundDocs {

        @Test
        public void resolveCompoundDocs_includePathsReachingSameType_includesOnlyResourcesOnRequestedPaths() throws Exception {
            CompoundDocsResolver sut = resolver(3, true);
            stubDownstream();

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    primaryDoc(USER_1, Set.of("citizenships", "placeOfBirth")),
                    request("/users/1", "citizenships", "placeOfBirth.currencies")
            );

            assertThat(included(result)).containsExactlyInAnyOrder(NORWAY, USA, USD);
            assertThat(fetchCalls).containsExactlyInAnyOrder(
                    new FetchCall("countries", Set.of("NO", "US"), Set.of("currencies")),
                    new FetchCall("currencies", Set.of("USD"), Set.of())
            );
        }

        @Test
        public void resolveCompoundDocs_relationshipDoc_followsIncludesFromRelationshipName() throws Exception {
            CompoundDocsResolver sut = resolver(3, true);
            stubDownstream();

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    "{\"data\":[{\"type\":\"countries\",\"id\":\"NO\"},{\"type\":\"countries\",\"id\":\"US\"}]}",
                    request("/users/1/relationships/citizenships", "citizenships.currencies")
            );

            assertThat(included(result)).containsExactlyInAnyOrder(NORWAY, USA, NOK, USD);
        }

        @Test
        public void resolveCompoundDocs_resourceReachedAgainNeedingMoreIncludes_refetchesItWithCombinedIncludes() throws Exception {
            CompoundDocsResolver sut = resolver(3, true);
            stubDownstream();

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    primaryDoc(USER_1, Set.of("relatives")),
                    request("/users/1", "relatives", "relatives.relatives.placeOfBirth")
            );

            assertThat(included(result)).containsExactlyInAnyOrder(USER_2, USER_3, NORWAY);
            assertThat(fetchCalls).containsExactly(
                    new FetchCall("users", Set.of("2", "3"), Set.of("relatives")),
                    new FetchCall("users", Set.of("3"), Set.of("relatives", "placeOfBirth")),
                    new FetchCall("countries", Set.of("NO"), Set.of())
            );
        }

        @Test
        public void resolveCompoundDocs_resourceReachedAgainWithCoveredIncludes_doesNotRefetchIt() throws Exception {
            CompoundDocsResolver sut = resolver(3, true);
            stubDownstream();

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    primaryDoc(USER_1, Set.of("relatives")),
                    request("/users/1", "relatives.relatives")
            );

            assertThat(included(result)).containsExactlyInAnyOrder(USER_2, USER_3);
            assertThat(fetchCalls).containsExactly(
                    new FetchCall("users", Set.of("2", "3"), Set.of("relatives"))
            );
        }

        @Test
        public void resolveCompoundDocs_includeDeeperThanMaxHops_stopsAtMaxHops() throws Exception {
            CompoundDocsResolver sut = resolver(1, true);
            stubDownstream();

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    primaryDoc(USER_1, Set.of("placeOfBirth")),
                    request("/users/1", "placeOfBirth.currencies")
            );

            assertThat(included(result)).containsExactly(USA);
        }

        @Test
        public void resolveCompoundDocs_linkageForRelationshipNotRequested_ignoresIt() throws Exception {
            CompoundDocsResolver sut = resolver(3, true);
            stubDownstream();

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    primaryDoc(USER_1, Set.of("relatives", "citizenships", "placeOfBirth")),
                    request("/users/1", "placeOfBirth")
            );

            assertThat(included(result)).containsExactly(USA);
        }

    }

    private CompoundDocsResolver resolver(int maxHops, boolean deduplicateResources) {
        CompoundDocsResolverConfig config = new CompoundDocsResolverConfig(
                true, maxHops, 100, ErrorStrategy.FAIL, List.of(), deduplicateResources, 1000, 1000, false, 1
        );
        return new CompoundDocsResolver(
                config,
                (resourceType, selfBaseUrl) -> DomainSettings.of(URI.create(selfBaseUrl)),
                MAPPER,
                executorService,
                fetcher
        );
    }

    @SuppressWarnings("unchecked")
    private void stubDownstream() {
        when(fetcher.fetch(any(), any(), any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            String type = invocation.getArgument(1);
            Set<String> ids = invocation.getArgument(2);
            Set<String> includes = invocation.getArgument(3);
            fetchCalls.add(new FetchCall(type, Set.copyOf(ids), Set.copyOf(includes)));
            List<String> resources = new ArrayList<>();
            for (String id : ids) {
                resources.add(MAPPER.writeValueAsString(resourceNode(idAndType(type, id), includes)));
            }
            return new BatchFetchResult(resources, null);
        });
    }

    private static CompoundDocsRequest request(String relativePath, String... includes) {
        return new CompoundDocsRequest(
                "GET",
                List.of(includes),
                Map.of(),
                Map.of(),
                relativePath,
                Map.of(),
                "http://localhost/jsonapi"
        );
    }

    private static String primaryDoc(IdAndType resource, Set<String> linkedRelationships) throws JsonProcessingException {
        ObjectNode document = MAPPER.createObjectNode();
        document.set("data", resourceNode(resource, linkedRelationships));
        return MAPPER.writeValueAsString(document);
    }

    private static ObjectNode resourceNode(IdAndType resource, Set<String> linkedRelationships) {
        ObjectNode node = MAPPER.createObjectNode()
                .put("type", resource.getType().getType())
                .put("id", resource.getId());
        ObjectNode relationships = node.putObject("relationships");
        DOWNSTREAM.getOrDefault(resource, Map.of()).forEach((relationshipName, linkage) -> {
            ObjectNode relationship = relationships.putObject(relationshipName);
            if (linkedRelationships.contains(relationshipName)) {
                ArrayNode data = relationship.putArray("data");
                linkage.forEach(target -> data.addObject().put("type", target.getType().getType()).put("id", target.getId()));
            }
        });
        return node;
    }

    private static List<IdAndType> included(CompoundDocsResult result) throws JsonProcessingException {
        List<IdAndType> included = new ArrayList<>();
        for (JsonNode node : MAPPER.readTree(result.responseBody()).path("included")) {
            included.add(idAndType(node.get("type").asText(), node.get("id").asText()));
        }
        return included;
    }

}
