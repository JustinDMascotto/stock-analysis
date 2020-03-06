package org.bde.chart.generator;

import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.bde.chart.generator.service.ImageGeneratorService;
import org.bde.chart.generator.service.HistoricalDataRetriever;
import org.bde.chart.generator.util.Bucket4JAppLocalRateLimiter;
import org.bde.chart.generator.util.IAppLocalRateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;


@Slf4j
@EnableConfigurationProperties
@SpringBootApplication
public class Application
      implements CommandLineRunner
{
    @Autowired
    private ImageGeneratorService imageGeneratorService;

    @Autowired
    private HistoricalDataRetriever dataRetriever;

    @Value( "${bde.stock-analysis.list-of-stocks}" )
    private List<String> tickers;


    public static void main( String[] args )
    {
        SpringApplication.run( Application.class, args );
    }


    @Override
    public void run( String... args )
          throws Exception
    {
        System.setProperty( "java.awt.headless", "false" );
        final IAppLocalRateLimiter apiRateLimiter = new Bucket4JAppLocalRateLimiter( 1,
                                                                                     Refill.greedy( 1, Duration.of( 30, ChronoUnit.SECONDS ) ),
                                                                                     Duration.of( 2, ChronoUnit.MINUTES ),
                                                                                     "ApiRateLimiter" );
        tickers.forEach( ticker -> {
            try
            {
                apiRateLimiter.call( () -> {
                    dataRetriever.maybeRetrieveData( ticker, 1 );
                    return null;
                } );
                //            imageGeneratorService.generateGraph();
            }
            catch ( final Exception ex )
            {
                log.error( "There was an issue while running the NMFTA update task.", ex );
            }
        } );

        System.exit( 0 );
    }
}
