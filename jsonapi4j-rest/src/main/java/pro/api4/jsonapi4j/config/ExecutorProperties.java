package pro.api4.jsonapi4j.config;

import lombok.Data;

@Data
public class ExecutorProperties {

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    public static final int JSONAPI4J_EXECUTOR_CORE_POOL_SIZE_DEFAULT_VALUE = Math.max(2, CPU_COUNT);
    public static final int JSONAPI4J_EXECUTOR_MAX_POOL_SIZE_DEFAULT_VALUE = JSONAPI4J_EXECUTOR_CORE_POOL_SIZE_DEFAULT_VALUE * 2;
    public static final int JSONAPI4J_EXECUTOR_QUEUE_CAPACITY_DEFAULT_VALUE = 512;
    public static final long JSONAPI4J_EXECUTOR_KEEP_ALIVE_SECONDS_DEFAULT_VALUE = 60L;
    public static final boolean JSONAPI4J_EXECUTOR_ALLOW_CORE_THREAD_TIMEOUT_DEFAULT_VALUE = true;
    public static final String JSONAPI4J_EXECUTOR_THREAD_NAME_PREFIX_DEFAULT_VALUE = "jsonapi4j-";

    private int corePoolSize = JSONAPI4J_EXECUTOR_CORE_POOL_SIZE_DEFAULT_VALUE;
    private int maxPoolSize = JSONAPI4J_EXECUTOR_MAX_POOL_SIZE_DEFAULT_VALUE;
    private int queueCapacity = JSONAPI4J_EXECUTOR_QUEUE_CAPACITY_DEFAULT_VALUE;
    private long keepAliveSeconds = JSONAPI4J_EXECUTOR_KEEP_ALIVE_SECONDS_DEFAULT_VALUE;
    private boolean allowCoreThreadTimeout = JSONAPI4J_EXECUTOR_ALLOW_CORE_THREAD_TIMEOUT_DEFAULT_VALUE;
    private String threadNamePrefix = JSONAPI4J_EXECUTOR_THREAD_NAME_PREFIX_DEFAULT_VALUE;
}
