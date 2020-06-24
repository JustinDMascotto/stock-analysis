package org.bde.chart.generator.service.ruleengines;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bde.chart.generator.entity.MarketOpen;
import org.bde.chart.generator.entity.StockCandleEntity;
import org.bde.chart.generator.entity.TickerEntity;
import org.bde.chart.generator.repository.StockCandleRepository;
import org.bde.chart.generator.service.component.CandlestickChart;
import org.bde.chart.generator.service.component.ImageContainer;
import org.bde.chart.generator.util.CandleConverterUtil;

import javax.imageio.ImageIO;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toMap;

@Slf4j
public abstract class AbstractBuySellRuleEngine
{
    private final StockCandleRepository stockCandleRepository;

    private final String outputFolder;


    AbstractBuySellRuleEngine( final StockCandleRepository stockCandleRepository,
                               final String outputFolder )
    {
        this.stockCandleRepository = stockCandleRepository;
        this.outputFolder = outputFolder;
    }


    public void analyzeStock( final TickerEntity ticker,
                              final Integer inputInterval,
                              final LocalDateTime startDateTime,
                              final LocalDateTime endDateTime )
    {
        var syntheticNow = startDateTime;
        Optional<RuleEngineOutput> buyOrSellOutput = Optional.empty();
        while ( syntheticNow.isBefore( endDateTime ) )
        {
            if ( ticker.getMarketOpen() == MarketOpen.CONTINUOUS )
            {
                final var candles =
                      stockCandleRepository.findByTickerAndIntervalBetweenDateTimes( syntheticNow.minus( getCandleWindowDuration() ),
                                                                                     syntheticNow,
                                                                                     ticker.getName(),
                                                                                     inputInterval );

                final var convertedCandles = convertCandles( candles, inputInterval );

                final var currentCandle = convertedCandles.entrySet().iterator().next().getValue();
                var tempSynthNow = syntheticNow;

                final CandlestickChart chart = new CandlestickChart();
                IntStream.range( 0, (int) getCandleWindowDuration().toMinutes() )
                         .forEach( i -> {
                             final LocalDateTime timeOfCandle = tempSynthNow.minusMinutes( i );
                             Optional.ofNullable( convertedCandles.get( timeOfCandle ) )
                                     .ifPresent( candle -> chart.addCandle( candle, getCandleWindowDuration().toMinutes() - ( i + 1 ) ) );
                         } );

                final ImageContainer container = new ImageContainer( Arrays.asList( chart ) );

                final StockCandleEntity lookaheadCandle = stockCandleRepository.findByTickerIntervalAndDateTime( syntheticNow.plus( getLookAheadDuration() ),
                                                                                                                 ticker.getName(),
                                                                                                                 inputInterval );

                final StockCandleEntity previousCandle = stockCandleRepository.findByTickerIntervalAndDateTime( syntheticNow.minus( getLookAheadDuration() ),
                                                                                                                ticker.getName(),
                                                                                                                inputInterval );

                if ( currentCandle != null &&
                     previousCandle != null &&
                     lookaheadCandle != null )
                {
                    buyOrSellOutput = Optional.of( determineBuyOrSell( currentCandle, previousCandle, lookaheadCandle ) );

                    toImage( container.getContentPane(),
                             StringUtils.appendIfMissing( outputFolder, "/", "/" ) +
                             buyOrSellOutput.get().getSubfolderName() + currentCandle.getTicker().getName() + "_" + currentCandle.getTimestamp() + ".png" );
                }
            }
            else
            {
                if ( syntheticNow.toLocalTime().isBefore( ticker.getMarketCloseTime().toLocalTime().minus( getLookAheadDuration() ) ) )
                {
                    // generate candles
                }
                else
                {
                    // move synthetic time to next day market open
                }
            }

            buyOrSellOutput.ifPresent( buyOrSell -> log.info( buyOrSell.name() ) );
            syntheticNow = syntheticNow.plus( getLookAheadDuration() );
        }
    }


    public LinkedHashMap<LocalDateTime, StockCandleEntity> convertCandles( final List<StockCandleEntity> candles,
                                                                           final Integer inputInterval )
    {
        final LinkedHashMap<LocalDateTime, StockCandleEntity> initialCandleTimeframeSorted =
              candles.stream()
                     .sorted( compareLocalDateTimes )
                     .collect( toMap( StockCandleEntity::getTimestamp, e -> e, ( e1, e2 ) -> e1, LinkedHashMap::new ) );

        CandleConverterUtil.fillInMissingCandles( initialCandleTimeframeSorted, inputInterval );

        return CandleConverterUtil.convertCandlesAndOrder( initialCandleTimeframeSorted, getOutputInterval() );
    }


    abstract protected RuleEngineOutput determineBuyOrSell( final StockCandleEntity currentCandle,
                                                            final StockCandleEntity previousCandle,
                                                            final StockCandleEntity lookAheadCandle );

    abstract protected Duration getCandleWindowDuration();

    abstract protected Duration getLookAheadDuration();

    abstract protected Integer getOutputInterval();


    void toImage( final Container imageContentPane,
                  final String savePath )
    {
        final Dimension size = imageContentPane.getSize();
        final BufferedImage image = new BufferedImage( size.width, size.height - 20, BufferedImage.TYPE_INT_RGB );
        final Graphics2D g2 = image.createGraphics();
        imageContentPane.paint( g2 );
        try
        {
            ImageIO.write( image, "png", new File( savePath ) );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
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
