package pro.api4.jsonapi4j.init;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.compatibility.JsonApi4jCompatibilityMode;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.servlet.JsonApi4jDispatcherServlet;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.JSONAPI4J_ATT_NAME;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.JSONAPI4J_PROPERTIES_ATT_NAME;

public class JsonApi4jServletContainerInitializerCompatibilityTests {

    @Test
    public void legacyModeFromConfig_isHonoredFromInitializerToDispatcher() throws Exception {
        String previousConfigPath = System.getProperty("jsonapi4j.config");
        System.setProperty("jsonapi4j.config", "/jsonapi4j-legacy-test.yaml");
        try {
            Map<String, Object> attributes = new HashMap<>();
            ServletContext servletContext = mock(ServletContext.class);

            when(servletContext.getAttribute(anyString())).thenAnswer(invocation -> attributes.get(invocation.getArgument(0)));
            doAnswer(invocation -> {
                attributes.put(invocation.getArgument(0), invocation.getArgument(1));
                return null;
            }).when(servletContext).setAttribute(anyString(), any());

            ServletRegistration.Dynamic servletRegistration = mock(ServletRegistration.Dynamic.class);
            when(servletContext.addServlet(anyString(), any(Servlet.class))).thenReturn(servletRegistration);

            FilterRegistration.Dynamic filterRegistration = mock(FilterRegistration.Dynamic.class);
            when(servletContext.addFilter(anyString(), any(Filter.class))).thenReturn(filterRegistration);

            JsonApi4jServletContainerInitializer initializer = new JsonApi4jServletContainerInitializer();
            initializer.onStartup(Collections.emptySet(), servletContext);

            JsonApi4jProperties properties = (JsonApi4jProperties) attributes.get(JSONAPI4J_PROPERTIES_ATT_NAME);
            assertThat(properties).isNotNull();
            assertThat(properties.getCompatibility().isLegacyMode()).isTrue();

            JsonApi4jDispatcherServlet dispatcherServlet = new JsonApi4jDispatcherServlet();
            ServletConfig servletConfig = mock(ServletConfig.class);
            when(servletConfig.getServletContext()).thenReturn(servletContext);

            dispatcherServlet.init(servletConfig);

            Field jsonApi4jField = JsonApi4jDispatcherServlet.class.getDeclaredField("jsonApi4j");
            jsonApi4jField.setAccessible(true);
            JsonApi4j dispatcherJsonApi4j = (JsonApi4j) jsonApi4jField.get(dispatcherServlet);

            assertThat(dispatcherJsonApi4j).isNotNull();
            assertThat(dispatcherJsonApi4j.getCompatibilityMode()).isEqualTo(JsonApi4jCompatibilityMode.LEGACY);
            assertThat(((JsonApi4j) attributes.get(JSONAPI4J_ATT_NAME)).getCompatibilityMode())
                    .isEqualTo(JsonApi4jCompatibilityMode.LEGACY);
        } finally {
            if (previousConfigPath == null) {
                System.clearProperty("jsonapi4j.config");
            } else {
                System.setProperty("jsonapi4j.config", previousConfigPath);
            }
        }
    }
}
