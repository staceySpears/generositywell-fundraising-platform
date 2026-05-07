package com.kenzie.appserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ConcurrentLinkedQueue;

@Configuration
/** Spring configuration for the shared thread pool used by async tasks. */
public class ExecutorServiceConfig {

    /**
     * Fixed-size thread pool (4 threads) used by {@code @Async} methods and other
     * background tasks. Named threads appear as {@code default_task_executor_thread-N} in logs.
     *
     * @return the TaskExecutor bean
     */
    @Bean
    public TaskExecutor executorService() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setThreadNamePrefix("default_task_executor_thread");
        executor.initialize();
        return executor;
    }
}
