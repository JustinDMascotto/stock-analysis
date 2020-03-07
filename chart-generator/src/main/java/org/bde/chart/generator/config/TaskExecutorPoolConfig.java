package org.bde.chart.generator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;


@Configuration
public class TaskExecutorPoolConfig
{
    @Bean
    TaskExecutor chartImageGeneratorExecutor()
    {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setQueueCapacity( 1000 );
        executor.setCorePoolSize( 50 );
        executor.setMaxPoolSize( 50 );
        executor.setRejectedExecutionHandler( new ThreadPoolExecutor.CallerRunsPolicy() );
        executor.setThreadNamePrefix( "stock_chart_gen-" );
        executor.initialize();
        return executor;
    }
}
