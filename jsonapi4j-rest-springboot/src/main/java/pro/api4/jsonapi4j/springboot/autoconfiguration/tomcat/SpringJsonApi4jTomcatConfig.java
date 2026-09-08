package pro.api4.jsonapi4j.springboot.autoconfiguration.tomcat;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tomcat-specific connector tuning, kept out of the main auto-configuration so that the Spring integration stays
 * container-neutral. Tomcat rejects unencoded {@code [} and {@code ]} in query strings, which JSON:API relies on for
 * {@code filter[...]} and {@code fields[...]} parameters; Jetty, Undertow and the Quarkus integration accept them as
 * they are and need no equivalent.
 *
 * <p>Isolating this in its own {@link ConditionalOnClass}-guarded class matters beyond tidiness: any reference to a
 * Tomcat type from the main configuration class forces Spring to resolve it while introspecting bean methods, which
 * fails the whole auto-configuration on a non-Tomcat container.
 *
 * <p>The guard names {@link Connector} rather than only {@link TomcatServletWebServerFactory}: the latter ships inside
 * the {@code spring-boot} jar and is therefore present even when Tomcat itself is not, so it cannot tell the two apart.
 */
@ConditionalOnClass(value = {Connector.class, TomcatServletWebServerFactory.class})
@Configuration
public class SpringJsonApi4jTomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> jsonApi4jTomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            //Configuring Tomcat to allow '[' and ']' chars in query params
            connector.setProperty("relaxedQueryChars", "[]");
        });
    }

}
