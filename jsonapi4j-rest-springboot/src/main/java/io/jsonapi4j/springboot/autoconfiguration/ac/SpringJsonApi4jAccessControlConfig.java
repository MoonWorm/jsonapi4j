package io.jsonapi4j.springboot.autoconfiguration.ac;

import io.jsonapi4j.plugin.ac.AccessControlEvaluator;
import io.jsonapi4j.plugin.ac.DefaultAccessControlEvaluator;
import io.jsonapi4j.plugin.ac.tier.AccessTierRegistry;
import io.jsonapi4j.plugin.ac.tier.DefaultAccessTierRegistry;
import io.jsonapi4j.servlet.filter.ac.DefaultPrincipalResolver;
import io.jsonapi4j.servlet.filter.ac.JsonApi4jAccessControlFilter;
import io.jsonapi4j.servlet.filter.ac.PrincipalResolver;
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
