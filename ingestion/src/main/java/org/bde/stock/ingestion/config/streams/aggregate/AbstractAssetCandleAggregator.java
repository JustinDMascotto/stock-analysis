package org.bde.stock.ingestion.config.streams.aggregate;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Suppressed;
import org.apache.kafka.streams.kstream.Window;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.kstream.Windows;
import org.apache.kafka.streams.kstream.internals.TimeWindow;
import org.apache.kafka.streams.state.WindowStore;
import org.bde.stock.common.message.AssetCandleMessageKey;
import org.bde.stock.common.message.AssetCandleMessageValue;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;


@Slf4j
public abstract class AbstractAssetCandleAggregator
{
    abstract Duration getWindowLength();

    abstract Integer getCompositeInterval();

    public final KStream<AssetCandleMessageKey, AssetCandleMessageValue> kstreamAssetCandleAgg( final KStream<AssetCandleMessageKey, AssetCandleMessageValue> candleStreamIn,
                                                                                                final String outputTopic )
    {
        final var valueSerde = new JsonSerde<>( AssetCandleMessageValue.class );

        final var outputStream = candleStreamIn.filter( ( ( key, value ) -> key.getInterval() == getCompositeInterval() ) )
                                               .groupBy( this::getKey,
                                                         Grouped.with( Serdes.String(), valueSerde ) )
                                               .windowedBy( window )
                                               .reduce( this::composeCandles,
                                                        Materialized.<String, AssetCandleMessageValue, WindowStore<Bytes, byte[]>>
                                                              as( "CandleAggStore" + getWindowLength() )
                                                              .withKeySerde( Serdes.String() )
                                                              .withValueSerde( valueSerde ) )
                                               .suppress( Suppressed.untilWindowCloses( Suppressed.BufferConfig.unbounded() ) )
                                               .toStream()
                                               .map( mapper );
        outputStream.to( outputTopic, Produced.with( new JsonSerde<>( AssetCandleMessageKey.class ), valueSerde ) );
        return outputStream;
    }


    private AssetCandleMessageValue composeCandles( final AssetCandleMessageValue value1,
                                                    final AssetCandleMessageValue value2 )
    {
        return AssetCandleMessageValue.builder()
                                      .timestamp( value1.getTimestamp().isAfter( value2.getTimestamp() ) ? value1.getTimestamp() : value2.getTimestamp() )
                                      .open( value1.getTimestamp().isBefore( value2.getTimestamp() ) ? value1.getOpen() : value2.getOpen() )
                                      .close( value1.getTimestamp().isAfter( value2.getTimestamp() ) ? value1.getClose() : value2.getClose() )
                                      .high( Math.max( value1.getHigh(), value2.getHigh() ) )
                                      .low( Math.min( value1.getLow(), value2.getLow() ) )
                                      .volume( value1.getVolume() + value2.getVolume() )
                                      .interval( (int) getWindowLength().toMinutes() )
                                      .vwap( ( value1.getVwap() != null && value2.getVwap() != null ) ? ( value1.getVwap() + value2.getVwap() ) / 2 : 0 )
                                      .build();
    }


    private String getKey( final AssetCandleMessageKey key,
                           final AssetCandleMessageValue value )
    {
        final var composedKey = key.getSymbol();
        log.debug( "Composed key: " + composedKey );
        return composedKey;
    }


    private final Windows<Window> window = new Windows<>()
    {
        @Override
        public Map<Long, Window> windowsFor( final long timestamp )
        {

            final var windowStart = timestamp % size() == 0 ? timestamp - ( timestamp % size() ) - size() : timestamp - ( timestamp % size() );
            final TimeWindow window = new TimeWindow( windowStart, windowStart + size() );
            if ( log.isDebugEnabled() )
            {
                log.debug( "Window start time: " + Instant.ofEpochMilli( windowStart ).atOffset( ZoneOffset.UTC ).toLocalDateTime().format( DateTimeFormatter.ISO_LOCAL_DATE_TIME ) +
                           " : " + Instant.ofEpochMilli( timestamp ).atOffset( ZoneOffset.UTC ).toLocalDateTime().format( DateTimeFormatter.ISO_LOCAL_DATE_TIME ) );
            }
            return Collections.singletonMap( windowStart, window );
        }


        @Override
        public long size()
        {
            return getWindowLength().toMillis();
        }


        @Override
        public long gracePeriodMs()
        {
            return 1000;
        }
    };

    private final KeyValueMapper<Windowed<String>, AssetCandleMessageValue, KeyValue<AssetCandleMessageKey, AssetCandleMessageValue>> mapper =
          ( key, value ) -> new KeyValue<>( AssetCandleMessageKey.builder()
                                                                 .interval( value.getInterval() )
                                                                 .symbol( key.key() ).build(),
                                            value );
}
