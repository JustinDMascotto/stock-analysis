package org.bde.chart.generator.config;

import io.github.bucket4j.Refill;
import org.bde.chart.generator.util.Bucket4JAppLocalRateLimiter;
import org.bde.chart.generator.util.IAppLocalRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;


@Configuration
public class ApiRateLimiterConfig
{
    @Bean
    public IAppLocalRateLimiter rateLimiter()
    {
        return new Bucket4JAppLocalRateLimiter( 1,
                                                Refill.greedy( 1, Duration.of( 30, ChronoUnit.SECONDS ) ),
                                                Duration.of( 2, ChronoUnit.MINUTES ),
                                                "ApiRateLimiter" );
    }
}
