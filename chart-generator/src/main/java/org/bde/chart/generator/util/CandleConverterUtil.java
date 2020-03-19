package org.bde.chart.generator.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.bde.chart.generator.entity.StockCandleEntity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toMap;


@Slf4j
public class CandleConverterUtil
{
    private static final LocalTime MARKET_OPEN = LocalTime.of( 9, 30, 0 );


    public static List<StockCandleEntity> convertCandlesAndOrder( final List<StockCandleEntity> candles,
                                                                  final Integer outputInterval )
    {
        return new ArrayList<>( convertCandlesAndOrder( candles.stream().collect( toMap( StockCandleEntity::getTimestamp, e -> e ) ), outputInterval ).values() );
    }


    public static LinkedHashMap<LocalDateTime, StockCandleEntity> convertCandlesAndOrder( final Map<LocalDateTime, StockCandleEntity> candleEntities,
                                                                                          final Integer outputInterval )
    {
        final Integer inputInterval =
              candleEntities.entrySet()
                            .stream()
                            .findFirst()
                            .orElseThrow( () -> new IllegalArgumentException( "Empty input candle map." ) )
                            .getValue().getInterval();

        if ( inputInterval > outputInterval )
        {
            throw new IllegalArgumentException( "Output candles cannot have a smaller interval then input candles." );
        }

        return candleEntities.entrySet()
                             .stream()
                             .sorted( compareLocalDateTimes )
                             .collect( toMap( Map.Entry::getKey, Map.Entry::getValue, ( e1, e2 ) -> e1, LinkedHashMap::new ) )
                             .entrySet()
                             .stream()
                             .filter( e -> e.getKey().getMinute() % outputInterval == 0 )
                             .map( e -> buildNewCandle( e.getValue(), outputInterval, candleEntities ) )
                             .filter( Objects::nonNull )
                             .collect( toMap( e -> (LocalDateTime) e.getKey(), e -> (StockCandleEntity) e.getValue(), ( e1, e2 ) -> e1, LinkedHashMap::new ) );
    }


    /**
     * Duplicate the pervious candle in the next candle spot to keep continuity.
     */
    public static void fillInMissingCandles( final LinkedHashMap<LocalDateTime, StockCandleEntity> candles,
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


    private static Map.Entry<LocalDateTime, StockCandleEntity> buildNewCandle( final StockCandleEntity candle,
                                                                               final Integer outputInterval,
                                                                               final Map<LocalDateTime, StockCandleEntity> candleEntityMap )
    {
        int numCandlesToMerge = outputInterval / candle.getInterval();
        final double[] tmpHigh = { candle.getHigh() };
        final double[] tmpLow = { candle.getLow() };
        final double[] tmpOpen = { 0 };
        final double tmpClose = candle.getClose();
        final int[] tmpVolume = { candle.getVolume() };
        final double[] vwap = { candle.getVwap() };
        final int[] divisor = { 1 };
        IntStream.range( 1, numCandlesToMerge )
                 .forEach( i -> {
                     final LocalDateTime candleTimestamp = candle.getTimestamp().minusMinutes( candle.getInterval() * i );
                     final Optional<StockCandleEntity> possibleMergeCandle = Optional.ofNullable( candleEntityMap.get( candleTimestamp ) );
                     if ( possibleMergeCandle.isPresent() )
                     {
                         divisor[0]++;
                         final var mergeCandle = possibleMergeCandle.get();
                         tmpHigh[0] = tmpHigh[0] > mergeCandle.getHigh() ? tmpHigh[0] : mergeCandle.getHigh();
                         tmpLow[0] = tmpLow[0] < mergeCandle.getLow() ? tmpLow[0] : mergeCandle.getLow();
                         vwap[0] = vwap[0] + ObjectUtils.defaultIfNull( mergeCandle.getVwap(), vwap[0] );
                         tmpVolume[0] = tmpVolume[0] + mergeCandle.getVolume();
                         if ( numCandlesToMerge - 1 == i )
                         {
                             tmpOpen[0] = mergeCandle.getOpen();
                         }
                     }
                     else
                     {
                         log.warn( "No candle with timestamp." );
                     }
                 } );

        return Map.entry( candle.getTimestamp(), StockCandleEntity.builder()
                                                                  .volume( tmpVolume[0] )
                                                                  .vwap( vwap[0] / divisor[0] )
                                                                  .close( tmpClose )
                                                                  .open( tmpOpen[0] )
                                                                  .high( tmpHigh[0] )
                                                                  .interval( outputInterval )
                                                                  .low( tmpLow[0] )
                                                                  .timestamp( candle.getTimestamp() )
                                                                  .ticker( candle.getTicker() ).build() );
    }


    private static Comparator<Map.Entry<LocalDateTime, StockCandleEntity>> compareLocalDateTimes = ( t2, t1 ) -> {
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
            return t2.getKey().isBefore( t1.getKey() ) ? 1 : -1;
        }
    };
}
