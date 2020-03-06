package org.bde.chart.generator.util;


import java.util.function.Supplier;


/**
 * Interface that provides a way to rate limit a call out to another API at the local application level.
 */
public interface IAppLocalRateLimiter
{
    /**
     * Applies a rate limit to a supplier, will throw {@link RateLimitException} if the rate limited function
     * blocks for a long enough period of time while waiting for permission to make the call
     * @param apiCall Supplier function making a call that is to be rate limited
     * @param <T> Return type of supplier
     * @return Result of the supplier function
     */
    <T> T call( Supplier<T> apiCall );
}
