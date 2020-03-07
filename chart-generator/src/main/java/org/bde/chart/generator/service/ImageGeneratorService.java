package org.bde.chart.generator.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bde.chart.generator.entity.StockCandleEntity;
import org.bde.chart.generator.repository.StockCandleRepository;
import org.bde.chart.generator.service.component.CandlestickChart;
import org.bde.chart.generator.service.component.ImageContainer;
import org.bde.chart.generator.util.CandleConverterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
public class ImageGeneratorService
{
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss" );

    private static final LocalTime MARKET_CLOSE = LocalTime.of( 16, 0, 0 );

    private static final String BUY_FOLDER = "Buy/";

    private static final String SELL_FOLDER = "Sell/";

    private static final String DO_NOTHING_FOLDER = "Nothing/";

    @Value( "${bde.stock-analysis.image-generator.output-dir}" )
    private String outputDir;

    @Autowired
    private StockCandleRepository repo;


    public void generateGraphs( final String ticker )
          throws IOException, InterruptedException
    {
        final StockCandleEntity earliestCandle = repo.findEarliestByTickerAndInterval( ticker, 1 );
        LocalDate queryDate = earliestCandle.getTimestamp().toLocalDate();
        final DayOfWeek dayOfWeek = DayOfWeek.from( queryDate );
        while ( dayOfWeek.getValue() < 6 &&
                !queryDate.isAfter( LocalDate.now() ) )
        {
            final List<StockCandleEntity> candles = repo.findByTimestampAndTicker( queryDate, ticker, 1 );

            if ( CollectionUtils.isNotEmpty( candles ) )
            {
                final LinkedHashMap<LocalDateTime, StockCandleEntity> oneMinuteCandleOrdered = candles.stream().sorted( compareLocalDateTimes ).collect( toMap( StockCandleEntity::getTimestamp, e -> e, ( e1, e2 ) -> e1, LinkedHashMap::new ) );

                fillInMissingCandles( oneMinuteCandleOrdered );
                final LinkedHashMap<LocalDateTime, StockCandleEntity> fiveMinuteCandleOrdered = CandleConverterUtil.convertCandlesAndOrder( oneMinuteCandleOrdered, 5 );
                final LinkedHashMap<LocalDateTime, StockCandleEntity> fifteenMinuteCandleOrdered = CandleConverterUtil.convertCandlesAndOrder( fiveMinuteCandleOrdered, 15 );

                final int fifteenMinuteCandleWindowSize = 10; //10 candles in images
                IntStream.range( 3, fifteenMinuteCandleOrdered.size() - fifteenMinuteCandleWindowSize - 1 )
                         .forEach( i -> createImage( fifteenMinuteCandleWindowSize, i, fiveMinuteCandleOrdered, fifteenMinuteCandleOrdered ) );
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


    private void createImage( final Integer fifteenMinuteCandleWindowSize,
                              final Integer offset,
                              final LinkedHashMap<LocalDateTime, StockCandleEntity> fiveMinuteCandles,
                              final LinkedHashMap<LocalDateTime, StockCandleEntity> fifteenMinuteCandles )
    {
        final List<StockCandleEntity> fifteenMinuteCandlesList = new ArrayList<>( fifteenMinuteCandles.values() );

        final StockCandleEntity currentClosed15MinuteCandle;
        final int minuteOffsetInFrame;
        if ( offset < fifteenMinuteCandleWindowSize )
        {
            currentClosed15MinuteCandle = fifteenMinuteCandlesList.get( fifteenMinuteCandles.size() - offset );
            minuteOffsetInFrame = offset * 15;
        }
        else
        {
            currentClosed15MinuteCandle = fifteenMinuteCandlesList.get( fifteenMinuteCandlesList.size() - ( fifteenMinuteCandleWindowSize + offset ) );
            minuteOffsetInFrame = fifteenMinuteCandleWindowSize * 15;
        }
        final StockCandleEntity lookAheadCandle = fifteenMinuteCandles.get( currentClosed15MinuteCandle.getTimestamp().plusMinutes( 15L ) );
        final CandlestickChart fiveMinuteCandleStickChart = new CandlestickChart();
        final CandlestickChart fifteenMinuteCandleStickChart = new CandlestickChart();

        IntStream.range( 0, minuteOffsetInFrame )
                 .forEach( i -> {
                     final LocalDateTime timeOfCandle = currentClosed15MinuteCandle.getTimestamp().minusMinutes( i );
                     Optional.ofNullable( fifteenMinuteCandles.get( timeOfCandle ) )
                             .ifPresent( candle -> fifteenMinuteCandleStickChart.addCandle( candle, (long) minuteOffsetInFrame - ( i + 1 ) ) );
                 } );

        IntStream.range( 0, minuteOffsetInFrame )
                 .forEach( i -> {
                     final LocalDateTime timeOfCandle = currentClosed15MinuteCandle.getTimestamp().minusMinutes( i );
                     Optional.ofNullable( fiveMinuteCandles.get( timeOfCandle ) )
                             .ifPresent( candle -> fiveMinuteCandleStickChart.addCandle( candle, (long) minuteOffsetInFrame - ( i + 1 ) ) );
                 } );

        final ImageContainer container = new ImageContainer( Arrays.asList( fifteenMinuteCandleStickChart ) );

        determineBuyOrSell( currentClosed15MinuteCandle, lookAheadCandle, container.getContentPane() );
    }


    private void determineBuyOrSell( final StockCandleEntity currentClosed15MinuteCandle,
                                     final StockCandleEntity lookAheadCandle,
                                     final Container container )
    {
        if ( currentClosed15MinuteCandle.getClose() > lookAheadCandle.getClose() )
        {
            //possible sell
            final double risk = currentClosed15MinuteCandle.getHigh() - currentClosed15MinuteCandle.getClose();
            if ( risk > 0 && risk > 0.2 )
            {
                if ( currentClosed15MinuteCandle.getClose() - lookAheadCandle.getClose() > 4 * risk )
                {
                    //sell
                    toImage( container,
                             StringUtils.appendIfMissing( outputDir, "/", "/" ) +
                             SELL_FOLDER + currentClosed15MinuteCandle.getTimestamp() + "_" + currentClosed15MinuteCandle.getTicker() + ".png" );

                    return;
                }
            }
        }
        else
        {
            //possible buy
            final double risk = currentClosed15MinuteCandle.getClose() - currentClosed15MinuteCandle.getLow();
            if ( risk > 0 && risk > 0.2 )
            {
                if ( lookAheadCandle.getClose() - currentClosed15MinuteCandle.getClose() > 4 * risk )
                {
                    //buy
                    toImage( container,
                             StringUtils.appendIfMissing( outputDir, "/", "/" ) +
                             BUY_FOLDER + currentClosed15MinuteCandle.getTimestamp() + "_" + currentClosed15MinuteCandle.getTicker() + ".png" );

                    return;
                }
            }
        }

        //TODO: Log info about buy and sell choice
        toImage( container,
                 StringUtils.appendIfMissing( outputDir, "/", "/" ) +
                 DO_NOTHING_FOLDER + currentClosed15MinuteCandle.getTimestamp() + "_" + currentClosed15MinuteCandle.getTicker() + ".png" );

        return;
    }


    private void toImage( final Container imageContentPane,
                          final String savePath )
    {
        final Dimension size = imageContentPane.getSize();
        final BufferedImage image = new BufferedImage( size.width, size.height, BufferedImage.TYPE_INT_RGB );
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


    /**
     * Duplicate the pervious candle in the next candle spot to keep continuity.
     */
    private void fillInMissingCandles( final LinkedHashMap<LocalDateTime, StockCandleEntity> oneMinuteCandles )
    {
        final List<StockCandleEntity> candles = new ArrayList<>( oneMinuteCandles.values() );
        candles.forEach( candle -> {
            final Optional<StockCandleEntity> nextCandle = Optional.ofNullable( oneMinuteCandles.get( candle.getTimestamp().plusMinutes( 1 ) ) );
            if ( candle.getTimestamp().toLocalTime().isBefore( MARKET_CLOSE ) &&
                 nextCandle.isEmpty() )
            {
                oneMinuteCandles.put( candle.getTimestamp().plusMinutes( 1 ), StockCandleEntity.builder()
                                                                                               .timestamp( candle.getTimestamp().plusMinutes( 1 ) )
                                                                                               .interval( candle.getInterval() )
                                                                                               .ticker( candle.getTicker() )
                                                                                               .vwap( candle.getVwap() )
                                                                                               .low( candle.getLow() )
                                                                                               .high( candle.getHigh() )
                                                                                               .close( candle.getClose() )
                                                                                               .open( candle.getOpen() )
                                                                                               .volume( candle.getVolume() ).build() );
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
