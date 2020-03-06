package org.bde.chart.generator.util;

import org.apache.commons.lang3.RandomUtils;
import org.bde.chart.generator.entity.StockCandleEntity;
import org.bde.chart.generator.model.Candle;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toMap;


public class CandleConverterUtilTest
{
    private static final LocalDateTime MARKET_CLOSE = LocalDateTime.of( 2020, 3, 6, 16, 0, 0 );


    @Test
    public void testConvertingCandles()
    {
        final Map<LocalDateTime, StockCandleEntity> candles =
              IntStream.range( 0, 390 )
                       .mapToObj( i ->
                                        StockCandleEntity.builder()
                                                         .interval( 1 )
                                                         .ticker( "AAPL" )
                                                         .volume( Long.valueOf( RandomUtils.nextLong( 100_000L, 200_000L ) ).intValue() )
                                                         .close( 110.00 )
                                                         .open( 99.00 )
                                                         .high( 130.12 )
                                                         .low( 94.87 )
                                                         .vwap( 93.2 )
                                                         .interval( 1 )
                                                         .timestamp( MARKET_CLOSE.minusMinutes( i ) ).build() )
                       .collect( toMap( StockCandleEntity::getTimestamp, e -> e ) );

        final Map<LocalDateTime, Candle> convertedCandles = CandleConverterUtil.convertCandles( candles, 5 );

        Assert.assertEquals( 78, convertedCandles.size() );
    }


    @Test
    public void testConvertingCandlesOpenClose()
    {
        final Map<LocalDateTime, StockCandleEntity> candles =
              IntStream.range( 0, 5 )
                       .mapToObj( i ->
                                        StockCandleEntity.builder()
                                                         .interval( 1 )
                                                         .ticker( "AAPL" )
                                                         .volume( Long.valueOf( RandomUtils.nextLong( 100_000L, 200_000L ) ).intValue() )
                                                         .close( 110.00 * ( i + 1 ) / 5 )
                                                         .open( 99.00 * ( i + 1 ) / 5 )
                                                         .high( 130.12 )
                                                         .low( 94.87 )
                                                         .vwap( 93.2 )
                                                         .interval( 1 )
                                                         .timestamp( MARKET_CLOSE.minusMinutes( i ) ).build() )
                       .collect( toMap( StockCandleEntity::getTimestamp, e -> e ) );

        final Map<LocalDateTime, Candle> convertedCandles = CandleConverterUtil.convertCandles( candles, 5 );

        Assert.assertEquals( 1, convertedCandles.size() );
        Assert.assertEquals( 99.0, convertedCandles.entrySet().stream().findFirst().get().getValue().getOpen(), 0.0 );
        Assert.assertEquals( 22.0, convertedCandles.entrySet().stream().findFirst().get().getValue().getClose(), 0.0 );
    }
}
