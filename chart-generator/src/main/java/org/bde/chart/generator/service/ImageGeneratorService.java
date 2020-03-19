package org.bde.chart.generator.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bde.chart.generator.entity.StockCandleEntity;
import org.bde.chart.generator.repository.StockCandleRepository;
import org.bde.chart.generator.service.ruleengines.FifteenMinuteVolumeRuleEngine;
import org.bde.chart.generator.service.ruleengines.RuleEngineOutput;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;


@Slf4j
@Service
public class ImageGeneratorService
{

    @Value( "${bde.stock-analysis.list-of-stocks}" )
    private List<String> tickers;

    @Autowired
    private StockCandleRepository repo;

    @Autowired
    @Qualifier( "chartImageGeneratorExecutor" )
    private TaskExecutor executor;

    @Autowired
    private FifteenMinuteVolumeRuleEngine fiveMinuteCandle15MinuteTimeFrameSimple;


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

        fiveMinuteCandle15MinuteTimeFrameSimple.analyzeStock( earliestCandle.getTicker(), inputCandleInterval, earliestCandle.getTimestamp().plus( fiveMinuteCandle15MinuteTimeFrameSimple.getCandleWindowDuration() ), LocalDateTime.now() );
    }


    //    void createChartsAsync( final List<StockCandleEntity> candles,
    //                            final LocalDate queryDate,
    //                            final String ticker,
    //                            final Integer inputCandleInterval,
    //                            final Integer outputCandleInterval )
    //    {
    //        CompletableFuture.supplyAsync( () -> {
    //                                           final LinkedHashMap<LocalDateTime, StockCandleEntity> initialCandleTimeframeSorted = candles.stream().sorted( compareLocalDateTimes ).collect( toMap( StockCandleEntity::getTimestamp, e -> e, ( e1, e2 ) -> e1, LinkedHashMap::new ) );
    //
    //                                           final LocalDate currentDate = queryDate;
    //                                           CandleConverterUtil.fillInMissingCandles( initialCandleTimeframeSorted,
    //                                                                                     inputCandleInterval );
    //
    //                                           final LinkedHashMap<LocalDateTime, StockCandleEntity> outputCandlesTimeframeSorted = CandleConverterUtil.convertCandlesAndOrder( initialCandleTimeframeSorted, outputCandleInterval );
    //                                           fiveMinuteCandle15MinuteTimeFrameSimple.createChartImages( ticker, currentDate, initialCandleTimeframeSorted, outputCandlesTimeframeSorted );
    //                                           return null;
    //                                       },
    //                                       executor );
    //    }



    private void cleanDirectory()
          throws IOException
    {
        final String outputDir = fiveMinuteCandle15MinuteTimeFrameSimple.getOutputDir();
        FileUtils.deleteDirectory( new File( outputDir ) );
        Stream.of( RuleEngineOutput.values() )
              .forEach( subFolder -> {
                  try
                  {
                      final Path subDir = Paths.get( StringUtils.appendIfMissing( outputDir, "/", "/" ) + subFolder.getSubfolderName() );
                      Files.createDirectories( subDir );
                  }
                  catch ( IOException e )
                  {
                      log.error( "Error making sub fodlers", e );
                  }
              } );
    }
}
