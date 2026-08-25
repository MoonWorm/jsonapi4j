package pro.api4.jsonapi4j.plugin.ac.anonymization;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
class ContainersTests {

    private static final List<Object> NO_KEYS = List.of();

    @Nested
    class Detection {

        @Test
        void isContainer_collection_isTrue() {
            assertThat(Containers.isContainer(List.of("a"))).isTrue();
        }

        @Test
        void isContainer_mapOptionalAndArray_areTrue() {
            assertThat(Containers.isContainer(Map.of())).isTrue();
            assertThat(Containers.isContainer(Optional.empty())).isTrue();
            assertThat(Containers.isContainer(new String[0])).isTrue();
        }

        @Test
        void isContainer_plainObject_isFalse() {
            assertThat(Containers.isContainer("a")).isFalse();
        }

        @Test
        void isContainerType_finalContainerType_isTrue() {
            assertThat(Containers.isContainerType(Optional.class)).isTrue();
        }

    }

    @Nested
    class Rebuilding {

        @Test
        void rebuild_list_producesAListWithTheKeptElements() {
            Object actualResult = Containers.rebuild(new ArrayList<>(List.of("a", "b")),
                    List.of("a"), NO_KEYS, List.class);

            assertThat(elementsOf(actualResult)).containsExactly("a");
        }

        @Test
        void rebuild_set_preservesEncounterOrder() {
            Object actualResult = Containers.rebuild(new LinkedHashSet<>(List.of("b", "a")),
                    List.of("b", "a"), NO_KEYS, Set.class);

            assertThat(elementsOf(actualResult)).containsExactly("b", "a");
        }

        @Test
        void rebuild_sortedSet_keepsItsComparator() {
            SortedSet<String> original = new TreeSet<>(Comparator.reverseOrder());
            original.addAll(List.of("a", "b"));

            Object actualResult = Containers.rebuild(original, List.of("a", "b"), NO_KEYS, SortedSet.class);

            assertThat(elementsOf(actualResult)).containsExactly("b", "a");
        }

        @Test
        void rebuild_sortedMap_keepsItsComparator() {
            SortedMap<String, String> original = new TreeMap<>(Comparator.reverseOrder());
            original.put("a", "1");
            original.put("b", "2");

            Object actualResult = Containers.rebuild(original,
                    List.of("1", "2"), List.of("a", "b"), SortedMap.class);

            assertThat(List.copyOf(((Map<?, ?>) actualResult).keySet().stream().map(Object::toString).toList()))
                    .containsExactly("b", "a");
        }

        @Test
        void rebuild_map_pairsKeptValuesWithTheirKeys() {
            Object actualResult = Containers.rebuild(new LinkedHashMap<>(Map.of("a", "1")),
                    List.of("1"), List.of("a"), Map.class);

            Map<Object, Object> rebuilt = (Map<Object, Object>) actualResult;

            assertThat(rebuilt).hasSize(1).containsEntry("a", "1");
        }

        @Test
        void rebuild_array_keepsTheComponentType() {
            Object actualResult = Containers.rebuild(new String[]{"a", "b"},
                    List.of("a"), NO_KEYS, String[].class);

            assertThat(actualResult).isInstanceOf(String[].class);
            assertThat((String[]) actualResult).containsExactly("a");
        }

        @Test
        void rebuild_optionalWithNothingKept_isEmpty() {
            Object actualResult = Containers.rebuild(Optional.of("a"), List.of(), NO_KEYS, Optional.class);

            assertThat((Optional<?>) actualResult).isEmpty();
        }

        @Test
        void rebuild_immutableValueDeclaredAsInterface_isReplacedWithAMutableOne() {
            Object actualResult = Containers.rebuild(List.of("a", "b"), List.of("a"), NO_KEYS, List.class);

            assertThat(elementsOf(actualResult)).containsExactly("a");
        }

    }

    @Nested
    class DeclaredTypeFallback {

        @Test
        void rebuild_declaredAsItsOwnTypeWithNoArgConstructor_isRebuiltAsThatType() {
            Bag<String> original = new Bag<>();
            original.add("a");

            Object actualResult = Containers.rebuild(original, List.of("a"), NO_KEYS, Bag.class);

            assertThat(actualResult).isInstanceOf(Bag.class);
        }

        @Test
        void rebuild_declaredAsItsOwnTypeWithoutNoArgConstructor_returnsNull() {
            Fixed<String> original = new Fixed<>(1);
            original.add("a");

            assertThat(Containers.rebuild(original, List.of("a"), NO_KEYS, Fixed.class)).isNull();
        }

        @Test
        void rebuild_noDeclaredTypeConstraint_usesTheStandardType() {
            Fixed<String> original = new Fixed<>(1);
            original.add("a");

            assertThat(Containers.rebuild(original, List.of("a"), NO_KEYS, null))
                    .isInstanceOf(ArrayList.class);
        }

    }

    private static List<Object> elementsOf(Object container) {
        return new ArrayList<>((Collection<Object>) container);
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

}
