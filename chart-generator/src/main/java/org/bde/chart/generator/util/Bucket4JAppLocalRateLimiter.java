package org.bde.chart.generator.util;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.bde.chart.generator.util.exception.RateLimitException;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;


/**
 * Rate limiting implementation using bucket4j. If a bucket of tokens is exhausted and another request is made,
 * a {@link RateLimitException} will be thrown.
 */
public class Bucket4JAppLocalRateLimiter
      implements IAppLocalRateLimiter
{
    private final Bucket bucket;

    private final Duration timeout;

    private final String rateLimiterName;


    public Bucket4JAppLocalRateLimiter( final long limit,
                                        final Duration periodDuration,
                                        final Duration timout,
                                        final String rateLimiterName )
    {
        final Bandwidth bandwidth = Bandwidth.simple( limit, periodDuration );
        this.bucket = Bucket4j.builder().addLimit( bandwidth ).build();
        this.timeout = timout;
        this.rateLimiterName = rateLimiterName;
    }


    public Bucket4JAppLocalRateLimiter( final long limit,
                                        final Refill refill,
                                        final Duration timeout,
                                        final String rateLimiterName )
    {
        final Bandwidth bandwidth = Bandwidth.classic( limit, refill );
        this.bucket = Bucket4j.builder().addLimit( bandwidth ).build();
        this.timeout = timeout;
        this.rateLimiterName = rateLimiterName;
    }


    @Override
    public <T> T call( final Supplier<T> apiCall )
    {
        final Instant startTime = Instant.now();
        while ( startTime.plus( timeout ).isAfter( Instant.now() ) )
        {
            if ( bucket.tryConsume( 1 ) )
            {
                return apiCall.get();
            }
        }

        throw new RateLimitException( String.format( "The rate limit for '%s' has been exceeded.", rateLimiterName ) );
    }
}