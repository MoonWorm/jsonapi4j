package pro.api4.jsonapi4j.config;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RawConfigAccessorTests {

    @Test
    void section_returnsNestedTraversal_forMapValue() {
        RawConfigAccessor root = new RawConfigAccessor(Map.of(JsonApi4jProperties.META_PROPERTY, Map.of("enabled", true, "baseUrl", "http://x")));

        assertThat(root.section(JsonApi4jProperties.META_PROPERTY)).isPresent();
        assertThat(root.section(JsonApi4jProperties.META_PROPERTY).flatMap(m -> m.value("enabled", Boolean.class))).contains(true);
        assertThat(root.section(JsonApi4jProperties.META_PROPERTY).flatMap(m -> m.strValue("baseUrl"))).contains("http://x");
    }

    @Test
    void section_isEmpty_whenAbsentOrNotAMap() {
        RawConfigAccessor root = new RawConfigAccessor(Map.of(JsonApi4jProperties.ROOT_PATH_PROPERTY, JsonApi4jProperties.DEFAULT_ROOT_PATH));

        assertThat(root.section("missing")).isEmpty();
        assertThat(root.section(JsonApi4jProperties.ROOT_PATH_PROPERTY)).isEmpty(); // present but a scalar, not a map
    }

    @Test
    void value_returnsTypedValue_whenInstanceOfType() {
        RawConfigAccessor root = new RawConfigAccessor(Map.of("enabled", true, "maxHops", 3));

        assertThat(root.value("enabled", Boolean.class)).contains(true);
        assertThat(root.value("maxHops", Integer.class)).contains(3);
        // supertype request also works (regression guard for the previous inverted isAssignableFrom check)
        assertThat(root.value("maxHops", Number.class)).contains(3);
    }

    @Test
    void value_isEmpty_whenAbsentNullOrWrongType() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("enabled", true);
        raw.put("nullable", null);
        RawConfigAccessor root = new RawConfigAccessor(raw);

        assertThat(root.value("missing", String.class)).isEmpty();
        assertThat(root.value("nullable", String.class)).isEmpty();
        assertThat(root.value("enabled", String.class)).isEmpty(); // Boolean requested as String
    }

    @Test
    void strValue_trimsAndTreatsBlankAsEmpty() {
        RawConfigAccessor root = new RawConfigAccessor(Map.of("a", "  hello  ", "blank", "   ", "notString", 1));

        assertThat(root.strValue("a")).contains("hello");
        assertThat(root.strValue("blank")).isEmpty();
        assertThat(root.strValue("notString")).isEmpty();
        assertThat(root.strValue("missing")).isEmpty();
    }

    @Test
    void boolValue_readsNativeBooleanOrParsesString() {
        RawConfigAccessor root = new RawConfigAccessor(
                Map.of("native", true, "yes", "true", "off", "off", "garbage", "maybe", "notText", 1));

        assertThat(root.boolValue("native")).contains(true);
        assertThat(root.boolValue("yes")).contains(true);
        assertThat(root.boolValue("off")).contains(false);
        assertThat(root.boolValue("garbage")).isEmpty();
        assertThat(root.boolValue("notText")).isEmpty();
        assertThat(root.boolValue("missing")).isEmpty();
    }

    @Test
    void numericValues_readNativeNumbersOrParseStrings() {
        RawConfigAccessor root = new RawConfigAccessor(
                Map.of("i", 3, "l", 100L, "d", 1.5, "asString", "42", "blank", "  ", "notNumber", "abc"));

        assertThat(root.intValue("i")).contains(3);
        assertThat(root.intValue("asString")).contains(42);
        assertThat(root.longValue("l")).contains(100L);
        assertThat(root.longValue("asString")).contains(42L);
        assertThat(root.doubleValue("d")).contains(1.5);
        assertThat(root.doubleValue("asString")).contains(42.0);

        assertThat(root.intValue("notNumber")).isEmpty();
        assertThat(root.intValue("blank")).isEmpty();
        assertThat(root.intValue("missing")).isEmpty();
    }

    @Test
    void nullBackingMap_isTreatedAsEmpty() {
        RawConfigAccessor root = new RawConfigAccessor(null);

        assertThat(root.section(JsonApi4jProperties.META_PROPERTY)).isEmpty();
        assertThat(root.value("x", String.class)).isEmpty();
        assertThat(root.strValue("x")).isEmpty();
    }

}
