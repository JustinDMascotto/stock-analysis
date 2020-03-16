package org.bde.chart.generator.service.rules;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bde.chart.generator.entity.StockCandleEntity;
import org.bde.chart.generator.service.component.CandlestickChart;
import org.bde.chart.generator.service.component.ImageContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import java.awt.Container;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;


@Slf4j
@Component
public class FiveMinuteCandle15MinuteTimeFrameSimple
      implements IBuySellRules
{

    @Value( "${bde.stock-analysis.image-generator.output-dir}" )
    private String outputDir;

    private static final String BUY_FOLDER = "Buy/";

    private static final String SELL_FOLDER = "Sell/";

    private static final String DO_NOTHING_FOLDER = "Nothing/";


    @Override
    public void createChartImages( final String ticker,
                                   final LocalDate candlesDate,
                                   final LinkedHashMap<LocalDateTime, StockCandleEntity> originalCandlesSorted,
                                   final LinkedHashMap<LocalDateTime, StockCandleEntity> composedCandlesSorted )
    {
        final int maxCandlesInWindow = this.getMaxCandlesInWindowLargesTimeFrame(); //6 candles in images
        IntStream.range( 3, originalCandlesSorted.size() - maxCandlesInWindow - 1 )
                 .forEach( i -> {
                     try
                     {
                         createImageAndDetermineBuyOrSell( maxCandlesInWindow, i, originalCandlesSorted, composedCandlesSorted );
                     }
                     catch ( final Exception ex )
                     {
                         log.error( "Exception while making chart for {} on {}",
                                    ticker, candlesDate );
                         log.error( ex.getMessage(), ex );
                     }
                 } );
    }


    @Override
    public void createImageAndDetermineBuyOrSell( final Integer numberOfCandlesInWindowHighestTimeFrame,
                                                  final Integer slidingWindowIndex,
                                                  final LinkedHashMap<LocalDateTime, StockCandleEntity> inputCandles,
                                                  final LinkedHashMap<LocalDateTime, StockCandleEntity> composedCandles )
    {
        {
            final List<StockCandleEntity> composeCandlesList = new ArrayList<>( composedCandles.values() );
            final Integer largerTimeframeInterval = composeCandlesList.get( 0 ).getInterval();
            final StockCandleEntity currentClosedComposedCandle;
            final int minutesInTimeFrame;
            if ( slidingWindowIndex < numberOfCandlesInWindowHighestTimeFrame )
            {
                currentClosedComposedCandle = composeCandlesList.get( composedCandles.size() - slidingWindowIndex );
                minutesInTimeFrame = slidingWindowIndex * largerTimeframeInterval;
            }
            else
            {
                currentClosedComposedCandle = composeCandlesList.get( composeCandlesList.size() - ( numberOfCandlesInWindowHighestTimeFrame + slidingWindowIndex ) );
                minutesInTimeFrame = numberOfCandlesInWindowHighestTimeFrame * largerTimeframeInterval;
            }
            final StockCandleEntity lookAheadCandle = composedCandles.get( currentClosedComposedCandle.getTimestamp().plusMinutes( largerTimeframeInterval ) );
            final CandlestickChart inputCandlesChart = new CandlestickChart();
            final CandlestickChart composedCandleChart = new CandlestickChart();

            IntStream.range( 0, minutesInTimeFrame )
                     .forEach( i -> {
                         final LocalDateTime timeOfCandle = currentClosedComposedCandle.getTimestamp().minusMinutes( i );
                         Optional.ofNullable( composedCandles.get( timeOfCandle ) )
                                 .ifPresent( candle -> composedCandleChart.addCandle( candle, (long) minutesInTimeFrame - ( i + 1 ) ) );
                     } );

            IntStream.range( 0, minutesInTimeFrame )
                     .forEach( i -> {
                         final LocalDateTime timeOfCandle = currentClosedComposedCandle.getTimestamp().minusMinutes( i );
                         Optional.ofNullable( inputCandles.get( timeOfCandle ) )
                                 .ifPresent( candle -> inputCandlesChart.addCandle( candle, (long) minutesInTimeFrame - ( i + 1 ) ) );
                     } );

            final ImageContainer container = new ImageContainer( Arrays.asList( inputCandlesChart ) );

            determineBuyOrSell( currentClosedComposedCandle, lookAheadCandle, container.getContentPane() );
        }
    }


    private void determineBuyOrSell( final StockCandleEntity currentClosed15MinuteCandle,
                                     final StockCandleEntity lookAheadCandle,
                                     final Container container )
    {
        final int minimumRiskMultiplier = 3;
        if ( currentClosed15MinuteCandle.getClose() > lookAheadCandle.getClose() )
        {
            //possible sell
            final double risk = ( currentClosed15MinuteCandle.getHigh() ) - ( currentClosed15MinuteCandle.getClose() < currentClosed15MinuteCandle.getOpen() ? currentClosed15MinuteCandle.getClose() : currentClosed15MinuteCandle.getOpen() );
            if ( risk > 0.3 )
            {
                if ( currentClosed15MinuteCandle.getClose() - lookAheadCandle.getClose() > minimumRiskMultiplier * risk )
                {
                    //sell
                    log.info( "Sell" );
                    toImage( container,
                             StringUtils.appendIfMissing( outputDir, "/", "/" ) +
                             SELL_FOLDER + currentClosed15MinuteCandle.getTicker() + "_" + currentClosed15MinuteCandle.getTimestamp() + ".png" );

                    return;
                }
            }
        }
        else
        {
            //possible buy
            final double risk = ( currentClosed15MinuteCandle.getClose() > currentClosed15MinuteCandle.getOpen() ? currentClosed15MinuteCandle.getClose() : currentClosed15MinuteCandle.getOpen() ) - currentClosed15MinuteCandle.getLow();
            if ( risk > 0.3 )
            {
                if ( lookAheadCandle.getClose() - currentClosed15MinuteCandle.getClose() > minimumRiskMultiplier * risk )
                {
                    //buy
                    log.info( "Buy" );
                    toImage( container,
                             StringUtils.appendIfMissing( outputDir, "/", "/" ) +
                             BUY_FOLDER + currentClosed15MinuteCandle.getTicker() + "_" + currentClosed15MinuteCandle.getTimestamp() + ".png" );

                    return;
                }
            }
        }

        //TODO: Log info about buy and sell choice
        toImage( container,
                 StringUtils.appendIfMissing( outputDir, "/", "/" ) +
                 DO_NOTHING_FOLDER + currentClosed15MinuteCandle.getTicker() + "_" + currentClosed15MinuteCandle.getTimestamp() + ".png" );
    }


    @Override
    public Integer getMaxCandlesInWindowLargesTimeFrame()
    {
        return 4;
    }
}
