package pro.api4.jsonapi4j.springboot.autoconfiguration.ac;

import pro.api4.jsonapi4j.plugin.ac.impl.AccessControlEvaluator;
import pro.api4.jsonapi4j.plugin.ac.impl.DefaultAccessControlEvaluator;
import pro.api4.jsonapi4j.plugin.ac.impl.tier.AccessTierRegistry;
import pro.api4.jsonapi4j.plugin.ac.impl.tier.DefaultAccessTierRegistry;
import pro.api4.jsonapi4j.servlet.filter.ac.DefaultPrincipalResolver;
import pro.api4.jsonapi4j.servlet.filter.ac.JsonApi4jAccessControlFilter;
import pro.api4.jsonapi4j.servlet.filter.ac.PrincipalResolver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringJsonApi4jAccessControlConfig {

    @Bean
    public AccessTierRegistry jsonapi4jAccessTierRegistry() {
        return new DefaultAccessTierRegistry();
    }

    @Bean
    public AccessControlEvaluator jsonapi4jAccessControlEvaluator(
            AccessTierRegistry accessTierRegistry
    ) {
        return new DefaultAccessControlEvaluator(accessTierRegistry);
    }

    @Bean
    public PrincipalResolver jsonapi4jPrincipalResolver(
            AccessTierRegistry accessTierRegistry
    ) {
        return new DefaultPrincipalResolver(accessTierRegistry);
    }

    @Bean
    public FilterRegistrationBean<?> jsonapi4jAccessControlFilter(
            PrincipalResolver jsonApi4jPrincipalResolver,
            @Qualifier("jsonApi4jDispatcherServlet") ServletRegistrationBean<?> jsonApi4jDispatcherServlet
    ) {
        return new FilterRegistrationBean<>(
                new JsonApi4jAccessControlFilter(jsonApi4jPrincipalResolver),
                jsonApi4jDispatcherServlet
        );
    }

}
