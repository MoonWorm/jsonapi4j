package pro.api4.jsonapi4j.plugin.oas;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.compatibility.JsonApi4jCompatibilityMode;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.JSONAPI4J_ATT_NAME;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.JSONAPI4J_PROPERTIES_ATT_NAME;
import static pro.api4.jsonapi4j.plugin.oas.init.JsonApiOasServletContainerInitializer.OAS_PLUGIN_PROPERTIES_ATT_NAME;
import static pro.api4.jsonapi4j.plugin.oas.init.JsonApiOasServletContainerInitializer.OAS_PLUGIN_ROOT_PATH_ATT_NAME;

public class OasServletCompatibilityModeTests {

    @Test
    public void init_prefersPropertiesCompatibilityModeOverJsonApi4jBeanMode() throws Exception {
        OasServlet sut = new OasServlet();
        ServletConfig config = mock(ServletConfig.class);
        ServletContext servletContext = mock(ServletContext.class);

        JsonApi4jProperties properties = new JsonApi4jProperties();
        properties.getCompatibility().setLegacyMode(true);

        JsonApi4j jsonApi4j = mock(JsonApi4j.class);
        when(jsonApi4j.getCompatibilityMode()).thenReturn(JsonApi4jCompatibilityMode.STRICT);
        when(jsonApi4j.getDomainRegistry()).thenReturn(mock(DomainRegistry.class));
        when(jsonApi4j.getOperationsRegistry()).thenReturn(mock(OperationsRegistry.class));

        when(config.getServletContext()).thenReturn(servletContext);
        when(servletContext.getAttribute(OAS_PLUGIN_ROOT_PATH_ATT_NAME)).thenReturn("/jsonapi");
        when(servletContext.getAttribute(OAS_PLUGIN_PROPERTIES_ATT_NAME)).thenReturn(new OasProperties());
        when(servletContext.getAttribute(JSONAPI4J_PROPERTIES_ATT_NAME)).thenReturn(properties);
        when(servletContext.getAttribute(JSONAPI4J_ATT_NAME)).thenReturn(jsonApi4j);

        sut.init(config);

        assertThat(getCompatibilityMode(sut)).isEqualTo(JsonApi4jCompatibilityMode.LEGACY);
    }

    @Test
    public void init_usesJsonApi4jBeanModeWhenPropertiesAreMissing() throws Exception {
        OasServlet sut = new OasServlet();
        ServletConfig config = mock(ServletConfig.class);
        ServletContext servletContext = mock(ServletContext.class);

        JsonApi4j jsonApi4j = mock(JsonApi4j.class);
        when(jsonApi4j.getCompatibilityMode()).thenReturn(JsonApi4jCompatibilityMode.LEGACY);
        when(jsonApi4j.getDomainRegistry()).thenReturn(mock(DomainRegistry.class));
        when(jsonApi4j.getOperationsRegistry()).thenReturn(mock(OperationsRegistry.class));

        when(config.getServletContext()).thenReturn(servletContext);
        when(servletContext.getAttribute(OAS_PLUGIN_ROOT_PATH_ATT_NAME)).thenReturn("/jsonapi");
        when(servletContext.getAttribute(OAS_PLUGIN_PROPERTIES_ATT_NAME)).thenReturn(new OasProperties());
        when(servletContext.getAttribute(JSONAPI4J_PROPERTIES_ATT_NAME)).thenReturn(null);
        when(servletContext.getAttribute(JSONAPI4J_ATT_NAME)).thenReturn(jsonApi4j);

        sut.init(config);

        assertThat(getCompatibilityMode(sut)).isEqualTo(JsonApi4jCompatibilityMode.LEGACY);
    }

    private static JsonApi4jCompatibilityMode getCompatibilityMode(OasServlet oasServlet) throws Exception {
        Field compatibilityMode = OasServlet.class.getDeclaredField("compatibilityMode");
        compatibilityMode.setAccessible(true);
        return (JsonApi4jCompatibilityMode) compatibilityMode.get(oasServlet);
    }
}
