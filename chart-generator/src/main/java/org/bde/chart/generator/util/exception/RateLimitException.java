package org.bde.chart.generator.util.exception;

public class RateLimitException
      extends RuntimeException
{
    public RateLimitException( final String message )
    {
        super( message );
    }

    public RateLimitException( final Throwable throwable )
    {
        super( throwable );
    }

    public RateLimitException( final String message,
                               final Throwable throwable )
    {
        super( message, throwable );
    }
}
