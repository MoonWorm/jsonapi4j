package pro.api4.jsonapi4j.plugin.ac.anonymization;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.plugin.ac.AnonymizationResult;
import pro.api4.jsonapi4j.plugin.ac.DefaultAccessControlEvaluator;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlScopes;
import pro.api4.jsonapi4j.plugin.ac.annotation.ScopesGroup;
import pro.api4.jsonapi4j.plugin.ac.context.DefaultAccessControlContext;
import pro.api4.jsonapi4j.plugin.ac.exception.AccessControlMisconfigurationException;
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForCustomClass;
import pro.api4.jsonapi4j.principal.AuthenticatedPrincipalContextHolder;
import pro.api4.jsonapi4j.principal.DefaultPrincipal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboundAnonymizerTests {

    private static final String SENSITIVE_SCOPE = "secret.read";

    private final DefaultAccessControlEvaluator sut = new DefaultAccessControlEvaluator();

    @AfterEach
    void clearPrincipal() {
        AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(null);
    }

    private void givenCallerWithoutTheScope() {
        AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(
                new DefaultPrincipal(List.of("SUPPORT"), Set.of(), "u1", Map.of()));
    }

    private void givenCallerWithTheScope() {
        AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(
                new DefaultPrincipal(List.of("SUPPORT"), Set.of(SENSITIVE_SCOPE), "u1", Map.of()));
    }

    private <T> AnonymizationResult<T> anonymize(T target) {
        return sut.anonymizeObjectIfNeeded(
                target,
                DefaultAccessControlContext.outboundForResource(target),
                OutboundAccessControlForCustomClass.fromClassAnnotationsOf(target));
    }

    @Nested
    class CollectionElements {

        @Test
        void anonymizeObjectIfNeeded_listElementFieldDenied_isHidden() {
            givenCallerWithoutTheScope();

            ListHolder actualResult = anonymize(new ListHolder()).targetObject();

            assertThat(actualResult.items.get(0).zip).isNull();
        }

        @Test
        void anonymizeObjectIfNeeded_listElementFieldAllowed_isReturned() {
            givenCallerWithTheScope();

            ListHolder actualResult = anonymize(new ListHolder()).targetObject();

            assertThat(actualResult.items.get(0).zip).isEqualTo("0150");
        }

        @Test
        void anonymizeObjectIfNeeded_arrayElementFieldDenied_isHidden() {
            givenCallerWithoutTheScope();

            assertThat(anonymize(new ArrayHolder()).targetObject().items[0].zip).isNull();
        }

        @Test
        void anonymizeObjectIfNeeded_mapValueFieldDenied_isHidden() {
            givenCallerWithoutTheScope();

            assertThat(anonymize(new MapHolder()).targetObject().byName.get("home").zip).isNull();
        }

        @Test
        void anonymizeObjectIfNeeded_optionalValueFieldDenied_isHidden() {
            givenCallerWithoutTheScope();

            assertThat(anonymize(new OptionalHolder()).targetObject().maybe.get().zip).isNull();
        }

        @Test
        void anonymizeObjectIfNeeded_nestedContainers_areTraversed() {
            givenCallerWithoutTheScope();

            NestedContainerHolder actualResult = anonymize(new NestedContainerHolder()).targetObject();

            assertThat(actualResult.byCity.get("oslo").get(0).zip).isNull();
        }

        @Test
        void anonymizeObjectIfNeeded_elementDeniedEntirely_isDroppedFromTheContainer() {
            givenCallerWithoutTheScope();

            assertThat(anonymize(new GuardedListHolder()).targetObject().items).isEmpty();
        }

        @Test
        void anonymizeObjectIfNeeded_elementDeniedEntirelyAndScopeGranted_isKept() {
            givenCallerWithTheScope();

            assertThat(anonymize(new GuardedListHolder()).targetObject().items).hasSize(2);
        }

        @Test
        void anonymizeObjectIfNeeded_containerFieldItselfDenied_hidesTheWholeContainer() {
            givenCallerWithoutTheScope();

            assertThat(anonymize(new GuardedContainerFieldHolder()).targetObject().items).isNull();
        }

        @Test
        void anonymizeObjectIfNeeded_containerElementDenied_originalContainerIsNotModified() {
            givenCallerWithoutTheScope();
            ListHolder original = new ListHolder();

            anonymize(original);

            assertThat(original.items.get(0).zip).isEqualTo("0150");
        }

    }

    @Nested
    class RuntimeTypes {

        @Test
        void anonymizeObjectIfNeeded_fieldDeclaredAsInterface_appliesTheRuntimeTypesRules() {
            givenCallerWithoutTheScope();

            assertThat(((CardPayment) anonymize(new InterfaceHolder()).targetObject().payment).pan).isNull();
        }

        @Test
        void anonymizeObjectIfNeeded_fieldDeclaredAsSupertype_appliesTheSubtypesRules() {
            givenCallerWithoutTheScope();

            assertThat(((HomeAddress) anonymize(new SupertypeHolder()).targetObject().address).doorCode).isNull();
        }

        @Test
        void anonymizeObjectIfNeeded_fieldDeclaredAsObject_appliesTheRuntimeTypesRules() {
            givenCallerWithoutTheScope();

            assertThat(((Address) anonymize(new ObjectHolder()).targetObject().loose).zip).isNull();
        }

        @Test
        void anonymizeObjectIfNeeded_subtypeInsideACollection_appliesBothItsOwnAndInheritedRules() {
            givenCallerWithoutTheScope();

            HomeAddress actualResult
                    = (HomeAddress) anonymize(new SubtypeListHolder()).targetObject().items.get(0);

            assertThat(actualResult.zip).isNull();
            assertThat(actualResult.doorCode).isNull();
        }

    }

    @Nested
    class CopyOnWrite {

        @Test
        void anonymizeObjectIfNeeded_nothingDenied_returnsTheOriginalInstance() {
            givenCallerWithTheScope();
            ListHolder original = new ListHolder();

            assertThat(anonymize(original).targetObject()).isSameAs(original);
        }

        @Test
        void anonymizeObjectIfNeeded_elementDenied_copiesOnlyThePathToIt() {
            givenCallerWithoutTheScope();
            SiblingHolder original = new SiblingHolder();
            Unguarded untouchedSibling = original.untouched;

            SiblingHolder actualResult = anonymize(original).targetObject();

            assertThat(actualResult).isNotSameAs(original);
            assertThat(actualResult.untouched).isSameAs(untouchedSibling);
        }

    }

    @Nested
    class Cycles {

        @Test
        void anonymizeObjectIfNeeded_selfReferentialGraph_terminates() {
            givenCallerWithoutTheScope();
            SelfReferential original = new SelfReferential();
            original.self = original;

            assertThat(anonymize(original).targetObject()).isNotNull();
        }

        @Test
        void anonymizeObjectIfNeeded_sameInstanceInTwoPlaces_isRedactedInBoth() {
            givenCallerWithoutTheScope();
            Address shared = new Address();
            SharedInstanceHolder original = new SharedInstanceHolder(shared);

            SharedInstanceHolder actualResult = anonymize(original).targetObject();

            assertThat(actualResult.first.zip).isNull();
            assertThat(actualResult.second.zip).isNull();
        }

    }

    @Nested
    class Paths {

        @Test
        void anonymizeObjectIfNeeded_listElementDenied_reportsAnIndexedPath() {
            givenCallerWithoutTheScope();

            assertThat(anonymize(new ListHolder()).anonymizedFields()).containsOnlyKeys("items[0].zip");
        }

        @Test
        void anonymizeObjectIfNeeded_mapValueDenied_reportsTheKey() {
            givenCallerWithoutTheScope();

            assertThat(anonymize(new MapHolder()).anonymizedFields()).containsOnlyKeys("byName[home].zip");
        }

        @Test
        void anonymizeObjectIfNeeded_droppedElement_reportsItsPosition() {
            givenCallerWithoutTheScope();

            assertThat(anonymize(new GuardedListHolder()).anonymizedFields())
                    .containsOnlyKeys("items[0]", "items[1]");
        }

    }

    @Nested
    class UnrebuildableContainers {

        @Test
        void anonymizeObjectIfNeeded_containerWithNoArgConstructor_isRebuiltAsItsOwnType() {
            givenCallerWithoutTheScope();

            RescuableHolder actualResult = anonymize(new RescuableHolder()).targetObject();

            assertThat(actualResult.items).isInstanceOf(Bag.class);
            assertThat(actualResult.items.get(0).zip).isNull();
        }

        @Test
        void anonymizeObjectIfNeeded_containerThatCannotBeRebuilt_throwsNamingTheDeclaredType() {
            givenCallerWithoutTheScope();

            assertThatThrownBy(() -> anonymize(new UnrescuableHolder()))
                    .isInstanceOf(AccessControlMisconfigurationException.class)
                    .hasMessageContaining("no-argument constructor")
                    .hasMessageContaining(Fixed.class.getName());
        }

        @Test
        void anonymizeObjectIfNeeded_containerThatCannotBeRebuiltButHidesNothing_passesThrough() {
            givenCallerWithTheScope();

            assertThat(anonymize(new UnrescuableHolder()).targetObject().items).hasSize(1);
        }

    }

    public static class Address {
        @AccessControl(scopes = @AccessControlScopes(@ScopesGroup(SENSITIVE_SCOPE)))
        public String zip = "0150";
        public String city = "Oslo";
    }

    public static class HomeAddress extends Address {
        @AccessControl(scopes = @AccessControlScopes(@ScopesGroup(SENSITIVE_SCOPE)))
        public String doorCode = "1234";
    }

    public interface Payment {
    }

    public static class CardPayment implements Payment {
        @AccessControl(scopes = @AccessControlScopes(@ScopesGroup(SENSITIVE_SCOPE)))
        public String pan = "4111";
    }

    @AccessControl(scopes = @AccessControlScopes(@ScopesGroup(SENSITIVE_SCOPE)))
    public static class Guarded {
        public String value = "G";
    }

    public static class ListHolder {
        public List<Address> items = new ArrayList<>(List.of(new Address()));
    }

    public static class ArrayHolder {
        public Address[] items = {new Address()};
    }

    public static class MapHolder {
        public Map<String, Address> byName = new LinkedHashMap<>(Map.of("home", new Address()));
    }

    public static class OptionalHolder {
        public Optional<Address> maybe = Optional.of(new Address());
    }

    public static class NestedContainerHolder {
        public Map<String, List<Address>> byCity
                = new LinkedHashMap<>(Map.of("oslo", new ArrayList<>(List.of(new Address()))));
    }

    public static class GuardedListHolder {
        public List<Guarded> items = new ArrayList<>(List.of(new Guarded(), new Guarded()));
    }

    public static class GuardedContainerFieldHolder {
        @AccessControl(scopes = @AccessControlScopes(@ScopesGroup(SENSITIVE_SCOPE)))
        public List<Address> items = new ArrayList<>(List.of(new Address()));
    }

    public static class SubtypeListHolder {
        public List<Address> items = new ArrayList<>(List.of(new HomeAddress()));
    }

    public static class InterfaceHolder {
        public Payment payment = new CardPayment();
    }

    public static class SupertypeHolder {
        public Address address = new HomeAddress();
    }

    public static class ObjectHolder {
        public Object loose = new Address();
    }

    public static class SiblingHolder {
        public List<Address> items = new ArrayList<>(List.of(new Address()));
        public Unguarded untouched = new Unguarded();
    }

    public static class Unguarded {
        public String label = "plain";
    }

    public static class SelfReferential {
        public SelfReferential self;
        public Address address = new Address();
    }

    public static class SharedInstanceHolder {
        public Address first;
        public Address second;

        public SharedInstanceHolder(Address shared) {
            this.first = shared;
            this.second = shared;
        }
    }

    public static class Bag<E> extends ArrayList<E> {
        public Bag() {
            super();
        }
    }

    public static class Fixed<E> extends ArrayList<E> {
        public Fixed(int capacity) {
            super(capacity);
        }
    }

    public static class RescuableHolder {
        public Bag<Address> items = new Bag<>();

        {
            items.add(new Address());
        }
    }

    public static class UnrescuableHolder {
        public Fixed<Address> items = new Fixed<>(1);

        {
            items.add(new Address());
        }
    }

}
