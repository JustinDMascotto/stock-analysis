package org.bde.chart.generator.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bde.chart.generator.entity.StockCandleEntity;
import org.bde.chart.generator.repository.StockCandleRepository;
import org.bde.chart.generator.service.rules.FiveMinuteCandle15MinuteTimeFrameSimple;
import org.bde.chart.generator.util.CandleConverterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;


@Slf4j
@Service
public class ImageGeneratorService
{

    @Value( "${bde.stock-analysis.image-generator.output-dir}" )
    private String outputDir;

    private static final String BUY_FOLDER = "Buy/";

    private static final String SELL_FOLDER = "Sell/";

    private static final String DO_NOTHING_FOLDER = "Nothing/";



    @Value( "${bde.stock-analysis.list-of-stocks}" )
    private List<String> tickers;

    @Autowired
    private StockCandleRepository repo;

    @Autowired
    @Qualifier( "chartImageGeneratorExecutor" )
    private TaskExecutor executor;

    @Autowired
    private FiveMinuteCandle15MinuteTimeFrameSimple fiveMinuteCandle15MinuteTimeFrameSimple;

    @Scheduled( initialDelay = 2000,
                fixedDelay = 4000000 )
    public void generateGraphs()
          throws Exception
    {
        System.setProperty( "java.awt.headless", "false" );

        cleanDirectory();

        tickers.forEach( ticker -> {
            try
            {
                generateGraphs( ticker, 5 );
            }
            catch ( Exception e )
            {
                log.error( "There was an error.", e );
            }
        } );

        log.info( "Completed generating images." );
    }


    void generateGraphs( final String ticker,
                         final Integer inputCandleInterval )
    {
        final StockCandleEntity earliestCandle = repo.findEarliestByTickerAndInterval( ticker, inputCandleInterval );
        LocalDate queryDate = earliestCandle.getTimestamp().toLocalDate();
        while ( !queryDate.isAfter( LocalDate.now() ) )
        {
            final List<StockCandleEntity> candles = repo.findByTimestampAndTicker( queryDate, ticker, inputCandleInterval );

            if ( CollectionUtils.isNotEmpty( candles ) )
            {
                createChartsAsync( candles, queryDate, ticker, inputCandleInterval, 15 );
            }
            else
            {
                log.warn( "No data for ticker {} on {}",
                          ticker,
                          queryDate );
            }
            queryDate = queryDate.plusDays( 1 );
        }
    }


    void createChartsAsync( final List<StockCandleEntity> candles,
                            final LocalDate queryDate,
                            final String ticker,
                            final Integer inputCandleInterval,
                            final Integer outputCandleInterval )
    {
        CompletableFuture.supplyAsync( () -> {
                                           final LinkedHashMap<LocalDateTime, StockCandleEntity> initialCandleTimeframeSorted = candles.stream().sorted( compareLocalDateTimes ).collect( toMap( StockCandleEntity::getTimestamp, e -> e, ( e1, e2 ) -> e1, LinkedHashMap::new ) );

                                           final LocalDate currentDate = queryDate;
                                           fillInMissingOneMinuteCandles( initialCandleTimeframeSorted,
                                                                          inputCandleInterval );

                                           final LinkedHashMap<LocalDateTime, StockCandleEntity> outputCandlesTimeframeSorted = CandleConverterUtil.convertCandlesAndOrder( initialCandleTimeframeSorted, outputCandleInterval );
                                           fiveMinuteCandle15MinuteTimeFrameSimple.createChartImages( ticker, currentDate, initialCandleTimeframeSorted, outputCandlesTimeframeSorted );
                                           return null;
                                       },
                                       executor );
    }


    /**
     * Duplicate the pervious candle in the next candle spot to keep continuity.
     */
    private void fillInMissingOneMinuteCandles( final LinkedHashMap<LocalDateTime, StockCandleEntity> candles,
                                                final Integer interval )
    {
        final List<StockCandleEntity> candlesList = new ArrayList<>( candles.values() );
        final Long windowLengthInMinutes = Duration.between( candlesList.get( candlesList.size() - 1 ).getTimestamp(), candlesList.get( 0 ).getTimestamp() ).toMinutes();
        final Long candlesInWindow = windowLengthInMinutes / interval;
        final StockCandleEntity originCandle = candlesList.get( 0 );
        IntStream.range( 0, candlesInWindow.intValue() )
                 .forEach( i -> {
                     final LocalDateTime previousCandlesTimestamp = originCandle.getTimestamp().minusMinutes( i * interval );

                     final Optional<StockCandleEntity> previousCandle = Optional.ofNullable( candles.get( previousCandlesTimestamp ) );
                     if ( previousCandle.isEmpty() )
                     {
                         final StockCandleEntity mockCandle = candles.get( previousCandlesTimestamp.plusMinutes( 1 ) );
                         candles.put( previousCandlesTimestamp, StockCandleEntity.builder()
                                                                  .timestamp( previousCandlesTimestamp )
                                                                  .interval( mockCandle.getInterval() )
                                                                  .ticker( mockCandle.getTicker() )
                                                                  .vwap( mockCandle.getVwap() )
                                                                  .low( mockCandle.getLow() )
                                                                  .high( mockCandle.getHigh() )
                                                                  .close( mockCandle.getClose() )
                                                                  .open( mockCandle.getOpen() )
                                                                  .volume( mockCandle.getVolume() ).build() );
                     }
                 } );
    }


    private void cleanDirectory()
          throws IOException
    {
        FileUtils.deleteDirectory( new File( outputDir ) );
        Stream.of( BUY_FOLDER, SELL_FOLDER, DO_NOTHING_FOLDER )
              .forEach( subFolder -> {
                  try
                  {
                      final Path subDir = Paths.get( StringUtils.appendIfMissing( outputDir, "/", "/" ) + subFolder );
                      Files.createDirectories( subDir );
                  }
                  catch ( IOException e )
                  {
                      log.error( "Error making sub fodlers", e );
                  }
              } );
    }


    private static Comparator<StockCandleEntity> compareLocalDateTimes = ( t2, t1 ) -> {
        if ( t2 == t1 )
        {
            return 0;
        }
        else if ( t2 == null )
        {
            return 1;
        }
        else if ( t1 == null )
        {
            return -1;
        }
        else
        {
            return t2.getTimestamp().isBefore( t1.getTimestamp() ) ? 1 : -1;
        }
    };
}
