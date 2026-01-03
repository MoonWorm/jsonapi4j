package pro.api4.jsonapi4j.processor;

import pro.api4.jsonapi4j.processor.multi.resource.MultipleResourcesProcessor;
import pro.api4.jsonapi4j.processor.multi.resource.MultipleResourcesDocSupplier;
import pro.api4.jsonapi4j.processor.single.resource.SingleResourceProcessor;
import pro.api4.jsonapi4j.processor.single.resource.SingleResourceDocSupplier;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.MultipleResourcesDoc;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static pro.api4.jsonapi4j.processor.resolvers.relationships.DefaultRelationshipResolvers.all;

@Disabled("for manual testing")
public class ConcurrentExecutorTests {

    private static final ResourceType TYPE1 = new ResourceType("mytype");

    private static final RelationshipName REL1_MULTI = new RelationshipName("rel1");
    private static final RelationshipName REL2_MULTI = new RelationshipName("rel2");
    private static final RelationshipName REL3_MULTI = new RelationshipName("rel3");
    private static final RelationshipName REL4_SINGLE = new RelationshipName("rel4");
    private static final RelationshipName REL5_SINGLE = new RelationshipName("rel5");
    private static final RelationshipName REL6_SINGLE = new RelationshipName("rel6");
    private static final RelationshipName REL7_SINGLE_BATCH = new RelationshipName("rel7");
    private static final RelationshipName REL8_MULTI_BATCH = new RelationshipName("rel8");

    private static final RelationshipName[] RELATIONSHIPS = new RelationshipName[] {
            REL1_MULTI,
            REL2_MULTI,
            REL3_MULTI,
            REL4_SINGLE,
            REL5_SINGLE,
            REL6_SINGLE,
            REL7_SINGLE_BATCH,
            REL8_MULTI_BATCH
    };


    @Test
    public void testParallelExecution_multiResources() {
        long start = System.currentTimeMillis();

        Request request = new Request(50,
                Set.of(
                        REL1_MULTI.getName(),
                        REL2_MULTI.getName(),
                        REL3_MULTI.getName(),
                        REL4_SINGLE.getName(),
                        REL5_SINGLE.getName(),
                        REL6_SINGLE.getName(),
                        REL7_SINGLE_BATCH.getName(),
                        REL8_MULTI_BATCH.getName()
                )
        );

        int THREADS_COUNT = 24;

        try (ExecutorService executorService = Executors.newFixedThreadPool(THREADS_COUNT)) {
            new MultipleResourcesProcessor()
                    .forRequest(request)
                    .concurrentRelationshipResolution(executorService)
                    .dataSupplier(req -> CursorPageableResponse.fromItemsAndCursor(List.of(1, 2, 3), null))
                    .defaultRelationships(all(TYPE1, String::valueOf, RELATIONSHIPS))
                    .toManyRelationshipResolver(REL1_MULTI, (req, dto) -> {
                        try {
                            Thread.sleep(2000L);
                            System.out.println(Thread.currentThread().getName() + ": Rel 1 (multi) resolved");
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        return new ToManyRelationshipsDoc(
                                List.of(new ResourceIdentifierObject("2", "secondtype")),
                                LinksObject.builder().self("http://self.link").build()
                        );
                    })
                    .toManyRelationshipResolver(REL2_MULTI, (req, dto) -> {
                        try {
                            Thread.sleep(2000L);
                            System.out.println(Thread.currentThread().getName() + " Rel 2 (multi) resolved");
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        return new ToManyRelationshipsDoc(
                                List.of(new ResourceIdentifierObject("3", "thirdtype")),
                                LinksObject.builder().self("http://self2.link").build()
                        );
                    })
                    .toManyRelationshipResolver(REL3_MULTI, (req, dto) -> {
                        try {
                            Thread.sleep(2000L);
                            System.out.println(Thread.currentThread().getName() + " Rel 3 (multi) resolved");
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        return new ToManyRelationshipsDoc(
                                List.of(new ResourceIdentifierObject("4", "fourthtype")),
                                LinksObject.builder().self("http://self3.link").build()
                        );
                    })
                    .toOneRelationshipResolver(REL4_SINGLE, (req, dto) -> {
                        try {
                            Thread.sleep(2000L);
                            System.out.println(Thread.currentThread().getName() + " Rel 4 (to one) resolved");
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        return new ToOneRelationshipDoc(
                                new ResourceIdentifierObject("5", "fifthtype"),
                                LinksObject.builder().self("http://self4.link").build()
                        );
                    })
                    .toOneRelationshipResolver(REL5_SINGLE, (req, dto) -> {
                        try {
                            Thread.sleep(2000L);
                            System.out.println(Thread.currentThread().getName() + " Rel 5 (to one) resolved");
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        return new ToOneRelationshipDoc(
                                new ResourceIdentifierObject("6", "sixthtype"),
                                LinksObject.builder().self("http://self5.link").build()
                        );
                    })
                    .toOneRelationshipResolver(REL6_SINGLE, (req, dto) -> {
                        try {
                            Thread.sleep(2000L);
                            System.out.println(Thread.currentThread().getName() + " Rel 6 (to one) resolved");
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        return new ToOneRelationshipDoc(
                                new ResourceIdentifierObject("7", "seventhtype"),
                                LinksObject.builder().self("http://self6.link").build()
                        );
                    })
                    .batchToOneRelationshipResolver(REL7_SINGLE_BATCH, (req, dto) -> {
                        try {
                            Thread.sleep(2000L);
                            System.out.println(Thread.currentThread().getName() + " Rel 7 (batch to one) resolved");
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        return Map.of(
                                1, new ToOneRelationshipDoc(new ResourceIdentifierObject("8", "eitthtype")),
                                2, new ToOneRelationshipDoc(new ResourceIdentifierObject("9", "eitthtype")),
                                3, new ToOneRelationshipDoc(new ResourceIdentifierObject("10", "eitthtype"))
                        );
                    })
                    .batchToManyRelationshipResolver(REL8_MULTI_BATCH, (req, dto) -> {
                        try {
                            Thread.sleep(2000L);
                            System.out.println(Thread.currentThread().getName() + " Rel 8 (batch multi) resolved");
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        return Map.of(
                                1,
                                new ToManyRelationshipsDoc(
                                        List.of(
                                                new ResourceIdentifierObject("11", "eleventhtype"),
                                                new ResourceIdentifierObject("12", "eleventhtype")
                                        )
                                ),
                                2,
                                new ToManyRelationshipsDoc(
                                        List.of(
                                                new ResourceIdentifierObject("13", "eleventhtype"),
                                                new ResourceIdentifierObject("14", "eleventhtype")
                                        )
                                ),
                                3,
                                new ToManyRelationshipsDoc(
                                        List.of(
                                                new ResourceIdentifierObject("15", "eleventhtype"),
                                                new ResourceIdentifierObject("16", "eleventhtype")
                                        )
                                )
                        );
                    })
                    .attributesResolver(suppliedId -> suppliedId)
                    .resourceTypeAndIdResolver(dto -> new IdAndType(String.valueOf(dto), TYPE1))
                    .toMultipleResourcesDoc(
                            MyRelationships::new,
                            new ResourceSupplier<Integer, MyRelationships, ResourceObject<Integer, MyRelationships>>() {
                                @Override
                                public ResourceObject<Integer, MyRelationships> get(String id, String type, Integer att, MyRelationships rel, LinksObject links, Object meta) {
                                    return new ResourceObject<>(id, type, att, rel, links, meta);
                                }
                            },
                            new MultipleResourcesDocSupplier<ResourceObject<Integer, MyRelationships>, MultipleResourcesDoc<ResourceObject<Integer, MyRelationships>>>() {

                                @Override
                                public MultipleResourcesDoc<ResourceObject<Integer, MyRelationships>> get(List<ResourceObject<Integer, MyRelationships>> data, LinksObject links, Object meta) {
                                    return new MultipleResourcesDoc<>(data, links, meta);
                                }
                            }
                    );
            System.out.println("Took: " + (System.currentTimeMillis() - start));
        }
    }

    @Test
    public void testParallelExecution_singleResource() {
        long start = System.currentTimeMillis();

        Request request = new Request(50, Set.of(
                REL1_MULTI.getName(),
                REL2_MULTI.getName(),
                REL3_MULTI.getName(),
                REL4_SINGLE.getName(),
                REL5_SINGLE.getName(),
                REL6_SINGLE.getName())
        );

        int THREADS_COUNT = 6;

        try (ExecutorService executorService = Executors.newFixedThreadPool(THREADS_COUNT)) {
            new SingleResourceProcessor()
                    .forRequest(request)
                    .concurrentRelationshipResolution(executorService)
                    .dataSupplier(req -> 2)
                    .defaultRelationships(all(TYPE1, String::valueOf, RELATIONSHIPS))
                    .toManyRelationshipResolver(REL1_MULTI, (req, dto) -> {
                        try {
                            Thread.sleep(2000L);
                            System.out.println(Thread.currentThread().getName() + ": Rel 1 (multi) resolved");
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        return new ToManyRelationshipsDoc(
                                List.of(new ResourceIdentifierObject("2", "secondtype")),
                                LinksObject.builder().self("http://self.link").build()
                        );
                    })
                    .toManyRelationshipResolver(REL2_MULTI, (req, dto) -> {
                        try {
                            Thread.sleep(2000L);
                            System.out.println(Thread.currentThread().getName() + " Rel 2 (multi) resolved");
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        return new ToManyRelationshipsDoc(
                                List.of(new ResourceIdentifierObject("3", "thirdtype")),
                                LinksObject.builder().self("http://self2.link").build()
                        );
                    })
                    .toManyRelationshipResolver(REL3_MULTI, (req, dto) -> {
                        try {
                            Thread.sleep(2000L);
                            System.out.println(Thread.currentThread().getName() + " Rel 3 (multi) resolved");
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        return new ToManyRelationshipsDoc(
                                List.of(new ResourceIdentifierObject("4", "fourthtype")),
                                LinksObject.builder().self("http://self3.link").build()
                        );
                    })
                    .toOneRelationshipResolver(REL4_SINGLE, (req, dto) -> {
                        try {
                            Thread.sleep(2000L);
                            System.out.println(Thread.currentThread().getName() + " Rel 4 (to one) resolved");
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        return new ToOneRelationshipDoc(
                                new ResourceIdentifierObject("5", "fifthtype"),
                                LinksObject.builder().self("http://self4.link").build()
                        );
                    })
                    .toOneRelationshipResolver(REL5_SINGLE, (req, dto) -> {
                        try {
                            Thread.sleep(2000L);
                            System.out.println(Thread.currentThread().getName() + " Rel 5 (to one) resolved");
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        return new ToOneRelationshipDoc(
                                new ResourceIdentifierObject("6", "sixthtype"),
                                LinksObject.builder().self("http://self5.link").build()
                        );
                    })
                    .toOneRelationshipResolver(REL6_SINGLE, (req, dto) -> {
                        try {
                            Thread.sleep(2000L);
                            System.out.println(Thread.currentThread().getName() + " Rel 6 (to one) resolved");
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        return new ToOneRelationshipDoc(
                                new ResourceIdentifierObject("7", "seventh"),
                                LinksObject.builder().self("http://self6.link").build()
                        );
                    })

                    .toOneRelationshipResolver(REL7_SINGLE_BATCH, (req, dto) -> {
                        try {
                            Thread.sleep(2000L);
                            System.out.println(Thread.currentThread().getName() + " Rel 7 (to one) resolved");
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        return new ToOneRelationshipDoc(
                                new ResourceIdentifierObject("8", "eight"),
                                LinksObject.builder().self("http://self7.link").build()
                        );
                    })
                    .toManyRelationshipResolver(REL8_MULTI_BATCH, (req, dto) -> {
                        try {
                            Thread.sleep(2000L);
                            System.out.println(Thread.currentThread().getName() + " Rel 8 (multi) resolved");
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        return new ToManyRelationshipsDoc(
                                List.of(new ResourceIdentifierObject("9", "nineth")),
                                LinksObject.builder().self("http://self8.link").build()
                        );
                    })
                    .attributesResolver(suppliedId -> suppliedId)
                    .resourceTypeAndIdResolver(dto -> new IdAndType(String.valueOf(dto), TYPE1))
                    .toSingleResourceDoc(
                            MyRelationships::new,
                            new ResourceSupplier<Integer, MyRelationships, ResourceObject<Integer, MyRelationships>>() {
                                @Override
                                public ResourceObject<Integer, MyRelationships> get(String id, String type, Integer att, MyRelationships rel, LinksObject links, Object meta) {
                                    return new ResourceObject<>(id, type, att, rel, links, meta);
                                }
                            },
                            new SingleResourceDocSupplier<ResourceObject<Integer, MyRelationships>, SingleResourceDoc<ResourceObject<Integer, MyRelationships>>>() {

                                @Override
                                public SingleResourceDoc<ResourceObject<Integer, MyRelationships>> get(ResourceObject<Integer, MyRelationships> data, LinksObject links, Object meta) {
                                    return new SingleResourceDoc<>(data, links, meta);
                                }
                            }
                    );
            System.out.println("Took: " + (System.currentTimeMillis() - start));
        }
    }

    @Data
    @AllArgsConstructor
    public class Request implements IncludeAwareRequest {

        private final int sourceId;
        private Set<String> includes;

        @Override
        public Set<String> getEffectiveIncludes() {
            return includes;
        }
    }

    @Data
    public class MyRelationships {

        private final ToManyRelationshipsDoc rel1;
        private final ToManyRelationshipsDoc rel2;
        private final ToManyRelationshipsDoc rel3;
        private final ToOneRelationshipDoc rel4;
        private final ToOneRelationshipDoc rel5;
        private final ToOneRelationshipDoc rel6;
        private final ToOneRelationshipDoc rel7;
        private final ToManyRelationshipsDoc rel8;

        public MyRelationships(
                Map<RelationshipName, ToManyRelationshipsDoc> toManyRelationshipsDocMap,
                Map<RelationshipName, ToOneRelationshipDoc> toOneRelationshipDocMap
        ) {
            this.rel1 = toManyRelationshipsDocMap.get(REL1_MULTI);
            this.rel2 = toManyRelationshipsDocMap.get(REL2_MULTI);
            this.rel3 = toManyRelationshipsDocMap.get(REL3_MULTI);
            this.rel4 = toOneRelationshipDocMap.get(REL4_SINGLE);
            this.rel5 = toOneRelationshipDocMap.get(REL5_SINGLE);
            this.rel6 = toOneRelationshipDocMap.get(REL6_SINGLE);
            this.rel7 = toOneRelationshipDocMap.get(REL7_SINGLE_BATCH);
            this.rel8 = toManyRelationshipsDocMap.get(REL8_MULTI_BATCH);
        }
    }

}
