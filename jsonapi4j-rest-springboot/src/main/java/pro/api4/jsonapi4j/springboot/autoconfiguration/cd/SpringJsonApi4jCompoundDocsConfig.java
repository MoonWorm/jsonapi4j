package pro.api4.jsonapi4j.springboot.autoconfiguration.cd;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pro.api4.jsonapi4j.filter.cd.CompoundDocsFilter;

@ConditionalOnProperty(
        prefix = "jsonapi4j.compound-docs",
        name = "enabled",
        havingValue = "true"
)
@Configuration
public class SpringJsonApi4jCompoundDocsConfig {

    @Bean
    public FilterRegistrationBean<?> jsonapi4jCompoundDocsFilter(
            @Qualifier("jsonApi4jDispatcherServlet") ServletRegistrationBean<?> jsonApi4jDispatcherServlet
    ) {
        return new FilterRegistrationBean<>(
                new CompoundDocsFilter(),
                jsonApi4jDispatcherServlet
        );
    }

}
