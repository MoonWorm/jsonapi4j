package pro.api4.jsonapi4j.springboot.autoconfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.meta.context.MetaContext;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.PluginRegistry;
import pro.api4.jsonapi4j.principal.DefaultPrincipalResolver;
import pro.api4.jsonapi4j.principal.PrincipalResolver;
import pro.api4.jsonapi4j.servlet.response.errorhandling.ErrorHandlerFactoriesRegistry;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.JSONAPI4J_DISPATCHER_SERVLET_NAME;

class SpringJsonApi4jAutoConfigurerTests {

    /*
     * The fixture configurations below are deliberately not annotated @Configuration. SpringJsonApi4jAutoConfigurer
     * component-scans pro.api4.jsonapi4j.springboot.autoconfiguration, which is this test's own package, so an
     * annotated fixture is picked up by every context rather than only the one that registers it. Passing them to
     * withUserConfiguration still processes their @Bean methods.
     */

    private final WebApplicationContextRunner sut = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    SpringJsonApi4jAutoConfigurer.class
            ));

    @Nested
    class CoreBeans {

        @Test
        void autoConfiguration_emptyContext_registersTheFrameworkBeans() {
            sut.run(context -> assertThat(context)
                    .hasSingleBean(JsonApi4j.class)
                    .hasSingleBean(DomainRegistry.class)
                    .hasSingleBean(OperationsRegistry.class)
                    .hasSingleBean(PluginRegistry.class)
                    .hasSingleBean(ErrorHandlerFactoriesRegistry.class)
                    .hasSingleBean(PrincipalResolver.class)
                    .hasBean("jsonApi4jObjectMapper")
                    .hasBean("jsonApi4jExecutorService")
                    .hasBean("jsonApi4jDispatcherServlet"));
        }

        @Test
        void autoConfiguration_emptyContext_registersTheDispatcherServletAndBothFilters() {
            sut.run(context -> assertThat(context)
                    .hasBean("jsonApi4jServletContextInitializer")
                    .hasBean("jsonApi4jDispatcherServlet")
                    .hasBean("jsonapi4jPrincipalResolvingFilter")
                    .hasBean("jsonApi4jRequestBodyCachingFilter"));
        }

        @Test
        void jsonapi4jPrincipalResolver_noUserBean_registersTheDefaultResolver() {
            sut.run(context -> assertThat(context.getBean(PrincipalResolver.class))
                    .isInstanceOf(DefaultPrincipalResolver.class));
        }
    }

    @Nested
    class DispatcherServletRegistration {

        @Test
        void jsonApi4jDispatcherServlet_defaultRootPath_mapsUnderJsonapi() {
            sut.run(context -> {
                ServletRegistrationBean<?> registration =
                        context.getBean("jsonApi4jDispatcherServlet", ServletRegistrationBean.class);

                assertThat(registration.getUrlMappings()).containsExactly("/jsonapi/*");
                assertThat(registration.getServletName()).isEqualTo(JSONAPI4J_DISPATCHER_SERVLET_NAME);
            });
        }

        @Test
        void jsonApi4jDispatcherServlet_customRootPath_mapsUnderThatPath() {
            sut.withPropertyValues("jsonapi4j.rootPath=/api")
                    .run(context -> assertThat(context
                            .getBean("jsonApi4jDispatcherServlet", ServletRegistrationBean.class)
                            .getUrlMappings()).containsExactly("/api/*"));
        }
    }

    @Nested
    class ObjectMapperOwnership {

        @Test
        void jsonApi4jObjectMapper_applicationDefinesItsOwnObjectMapper_frameworkKeepsItsOwn() {
            sut.withUserConfiguration(ApplicationObjectMapperConfig.class)
                    .run(context -> assertThat(context.getBean("jsonApi4jObjectMapper"))
                            .isNotSameAs(context.getBean("applicationObjectMapper")));
        }
    }

    @Nested
    class MetaContextRegistration {

        @Test
        void jsonApi4jMetaContext_metaDisabled_isNotRegistered() {
            sut.run(context -> assertThat(context).doesNotHaveBean(MetaContext.class));
        }

        @Test
        void jsonApi4jMetaContext_metaEnabled_isRegistered() {
            sut.withPropertyValues("jsonapi4j.meta.enabled=true")
                    .run(context -> assertThat(context).hasSingleBean(MetaContext.class));
        }
    }

    @Nested
    class UserBeansWin {

        @Test
        void jsonapi4jPrincipalResolver_userBean_backsOff() {
            sut.withUserConfiguration(UserPrincipalResolverConfig.class)
                    .run(context -> assertThat(context.getBean(PrincipalResolver.class))
                            .isSameAs(context.getBean("userPrincipalResolver")));
        }

        @Test
        void jsonApi4jDomainRegistry_userBean_backsOff() {
            sut.withUserConfiguration(UserDomainRegistryConfig.class)
                    .run(context -> assertThat(context.getBean(DomainRegistry.class))
                            .isSameAs(context.getBean("userDomainRegistry")));
        }

        @Test
        void jsonApi4jOperationsRegistry_userBean_backsOff() {
            sut.withUserConfiguration(UserOperationsRegistryConfig.class)
                    .run(context -> assertThat(context.getBean(OperationsRegistry.class))
                            .isSameAs(context.getBean("userOperationsRegistry")));
        }

        @Test
        void jsonApi4jExecutorService_userBean_backsOff() {
            sut.withUserConfiguration(UserExecutorServiceConfig.class)
                    .run(context -> assertThat(context.getBean("jsonApi4jExecutorService"))
                            .isSameAs(context.getBean(ExecutorService.class)));
        }
    }

    static class ApplicationObjectMapperConfig {

        @Bean
        ObjectMapper applicationObjectMapper() {
            return new ObjectMapper();
        }
    }

    static class UserPrincipalResolverConfig {

        @Bean
        PrincipalResolver userPrincipalResolver() {
            return servletRequest -> null;
        }
    }

    static class UserDomainRegistryConfig {

        @Bean
        DomainRegistry userDomainRegistry() {
            return DomainRegistry.empty();
        }
    }

    static class UserOperationsRegistryConfig {

        @Bean
        OperationsRegistry userOperationsRegistry() {
            return OperationsRegistry.builder(PluginRegistry.empty()).build();
        }
    }

    static class UserExecutorServiceConfig {

        @Bean(name = "jsonApi4jExecutorService")
        ExecutorService userExecutorService() {
            return Executors.newSingleThreadExecutor();
        }
    }
}
