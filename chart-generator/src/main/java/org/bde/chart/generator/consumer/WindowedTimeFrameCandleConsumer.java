package org.bde.chart.generator.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.kstream.Windowed;
import org.bde.chart.generator.render.CandlestickChart;
import org.bde.chart.generator.render.ImageContainer;
import org.bde.stock.common.message.AssetCandleMessageValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;


@Slf4j
@Component
public class WindowedTimeFrameCandleConsumer
{
    private static final Map<String, List<AssetCandleMessageValue>> previousWindows = new HashMap<>();

    @Value( "${org.bde.stock.window.five.length}" )
    private Integer fiveMinuteWindowLength;


    //TODO: make this consume a pattern of topics so we can consume any windowed timeframes and generate charts
    //TODO:
    @KafkaListener( containerFactory = "windowedAssetCandleListener",
                    topicPattern = "asset-candle.window.*" )
    public void consume( final ConsumerRecord<Windowed<String>, List<AssetCandleMessageValue>> message )
    {
        final var latestCandleTimestamp = getLatestCandlesTimestamp( message.value() );
        final var symbol = message.key().key();
        previousWindows.put( buildKey( latestCandleTimestamp, message ), message.value() );
        final var keyOfWindowToLabelAndPublish = getKeyOfWindowForHistoricDetermination( symbol, message.value() );
        if ( previousWindows.containsKey( keyOfWindowToLabelAndPublish ) )
        {
            labelImageAndPublish( keyOfWindowToLabelAndPublish );
        }

        generateGraphAndQueryModel( symbol, message.value() );
        log.info( "Consumed message " + message.timestamp() );
    }


    private Long getLatestCandlesTimestamp( final List<AssetCandleMessageValue> candles )
    {
        return candles.get( candles.size() - 1 ).getTimestamp().toInstant( ZoneOffset.UTC ).toEpochMilli();
    }


    private String buildKey( final Long timestamp,
                             final ConsumerRecord<Windowed<String>, List<AssetCandleMessageValue>> message )
    {
        return timestamp + ":" + message.value().get( 0 ).getInterval() + ":" + message.key().key();
    }


    private String getKeyOfWindowForHistoricDetermination( final String symbol,
                                                           final List<AssetCandleMessageValue> candles )
    {
        return candles.get( candles.size() - 3 )
                      .getTimestamp()
                      .toInstant( ZoneOffset.UTC )
                      .toEpochMilli() + ":" +
               candles.get( 0 ).getInterval() + ":" +
               symbol;
    }


    private void labelImageAndPublish( final String key )
    {
        log.info( String.valueOf( previousWindows.get( key ) ) );
    }


    private void generateGraphAndQueryModel( final String symbol,
                                             final List<AssetCandleMessageValue> candles )
    {
        if ( candles.size() == 5 )
        {
            final CandlestickChart chart = new CandlestickChart();
            IntStream.range( 1, candles.size() + 1 )
                     .forEach( i -> {
                         final var candle = candles.get( i - 1 );
                         chart.addCandle( candle, (long) i );
                     } );
            //                    chart.setBorder( null );
            //                    chart.setForeground( Color.WHITE );
            ImageContainer.toImage( new ImageContainer( Arrays.asList( chart ) ).getContentPane(), "/Users/justinmascotto/Downloads/images/" + symbol + "_" + candles.get( candles.size() - 1 ).getTimestamp().format( DateTimeFormatter.ofPattern( "yyyy.MM.dd'T'HH.mm.ss" ) ) + ".png" );
        }
    }
}
