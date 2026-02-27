package pro.api4.jsonapi4j.springboot.autoconfiguration;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringJsonApi4jAutoConfigurerExecutorTests {

    @Test
    public void defaultExecutor_isBoundedThreadPoolWithCallerRunsPolicy() {
        SpringJsonApi4jAutoConfigurer sut = new SpringJsonApi4jAutoConfigurer();
        JsonApi4jProperties properties = new JsonApi4jProperties();

        ExecutorService executorService = sut.jsonApi4jExecutorService(properties);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) executorService;

        assertThat(executor).isNotNull();
        assertThat(executor.getCorePoolSize()).isEqualTo(properties.getExecutor().getCorePoolSize());
        assertThat(executor.getMaximumPoolSize()).isEqualTo(properties.getExecutor().getMaxPoolSize());
        assertThat(executor.getQueue().remainingCapacity()).isEqualTo(properties.getExecutor().getQueueCapacity());
        assertThat(executor.getKeepAliveTime(TimeUnit.SECONDS)).isEqualTo(properties.getExecutor().getKeepAliveSeconds());
        assertThat(executor.getRejectedExecutionHandler()).isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);

        executor.shutdownNow();
    }

    @Test
    public void invalidExecutorSettings_areNormalizedToSafeMinimums() {
        SpringJsonApi4jAutoConfigurer sut = new SpringJsonApi4jAutoConfigurer();
        JsonApi4jProperties properties = new JsonApi4jProperties();
        properties.getExecutor().setCorePoolSize(0);
        properties.getExecutor().setMaxPoolSize(0);
        properties.getExecutor().setQueueCapacity(0);
        properties.getExecutor().setKeepAliveSeconds(-10);
        properties.getExecutor().setAllowCoreThreadTimeout(false);
        properties.getExecutor().setThreadNamePrefix(" ");

        ExecutorService executorService = sut.jsonApi4jExecutorService(properties);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) executorService;

        assertThat(executor.getCorePoolSize()).isEqualTo(1);
        assertThat(executor.getMaximumPoolSize()).isEqualTo(1);
        assertThat(executor.getQueue().remainingCapacity()).isEqualTo(1);
        assertThat(executor.getKeepAliveTime(TimeUnit.SECONDS)).isEqualTo(0);
        assertThat(executor.allowsCoreThreadTimeOut()).isFalse();

        executor.shutdownNow();
    }
}
