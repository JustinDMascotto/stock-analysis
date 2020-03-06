package org.bde.chart.generator;

import lombok.extern.slf4j.Slf4j;
import org.bde.chart.generator.service.ChartGeneratorService;
import org.bde.chart.generator.service.StockDataRetriever;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@Slf4j
@EnableConfigurationProperties
@SpringBootApplication
public class Application
      implements CommandLineRunner
{
    @Autowired
    private ChartGeneratorService chartGeneratorService;

    @Autowired
    private StockDataRetriever dataRetriever;

    public static void main( String[] args )
    {
        SpringApplication.run( Application.class, args );
    }

    @Override
    public void run( String... args )
          throws Exception
    {
        System.setProperty("java.awt.headless", "false");
        try
        {
            dataRetriever.maybeRetrieveData( "AAPL", 5 );
            chartGeneratorService.generateGraph();
        }
        catch ( final Exception ex )
        {
            log.error( "There was an issue while running the NMFTA update task.", ex );
        }
        // Exit 0 no matter what so kubernetes doesnt try to re-run the job indefinitely if there is an issue.
        System.exit( 0 );
    }
}
