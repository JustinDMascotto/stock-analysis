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
        executor.setQueueCapacity( 200 );
        executor.setCorePoolSize( 25 );
        executor.setMaxPoolSize( 25 );
        executor.setRejectedExecutionHandler( new ThreadPoolExecutor.CallerRunsPolicy() );
        executor.setThreadNamePrefix( "stock_chart_gen-" );
        executor.initialize();
        return executor;
    }
}
