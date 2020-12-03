package org.bde.stock.ingestion.streams.aggregate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.SessionWindows;
import org.apache.kafka.streams.kstream.Suppressed;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.kstream.WindowedSerdes;
import org.apache.kafka.streams.state.SessionStore;
import org.apache.kafka.streams.state.WindowStore;
import org.bde.stock.ingestion.message.AssetCandleCompactedMessageKey;
import org.bde.stock.ingestion.message.AssetCandleMessageKey;
import org.bde.stock.ingestion.message.AssetCandleMessageValue;
import org.bde.stock.ingestion.message.AssetCandleTableMessageValue;
import org.bde.stock.ingestion.streams.SourceAssetCandleStream;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;

@Slf4j
public abstract class AbstractAssetCandleAggregator
{
    abstract Integer getWindowLength();

    abstract Integer getCompositeInterval();


    public void kstreamAssetCandleAgg( final KStream<AssetCandleCompactedMessageKey, AssetCandleTableMessageValue> compacted )
    {
        final var windowLength = Duration.ofMinutes( getWindowLength() );
        final var valueSerde = new JsonSerde<>( AssetCandleTableMessageValue.class );

        final var windowSerde = WindowedSerdes.sessionWindowedSerdeFrom( String.class );
        final var groupedStream = compacted.filter( ( ( key, value ) -> key.getInterval() == getCompositeInterval() ) )
                                           .groupBy( this::getKey,
                                                     Grouped.with( Serdes.String(), valueSerde ) )
                                           .windowedBy( SessionWindows.with( Duration.ofMinutes( getWindowLength() ) )
                                                                      .grace( Duration.ofMinutes( 1 ) ) )
                                           .reduce( ( value1, value2 ) -> AssetCandleTableMessageValue.builder()
                                                                                                      .timestamp( value1.getTimestamp().isAfter( value2.getTimestamp() ) ? value1.getTimestamp() : value2.getTimestamp() )
                                                                                                      .open( value1.getTimestamp().isBefore( value2.getTimestamp() ) ? value1.getOpen() : value2.getOpen() )
                                                                                                      .close( value1.getTimestamp().isAfter( value2.getTimestamp() ) ? value1.getClose() : value2.getClose() )
                                                                                                      .high( Math.max( value1.getHigh(), value2.getHigh() ) )
                                                                                                      .low( Math.min( value1.getLow(), value2.getLow() ) )
                                                                                                      .volume( value1.getVolume() + value2.getVolume() )
                                                                                                      .interval( getWindowLength() )
                                                                                                      .vwap( ( value1.getVwap() != null && value2.getVwap() != null ) ? ( value1.getVwap() + value2.getVwap() ) / 2 : 0 )
                                                                                                      .build(),
                                                    Materialized.<String, AssetCandleTableMessageValue, SessionStore<Bytes, byte[]>>
                                                          as( "IntervalStore" + getWindowLength() )
                                                          .withKeySerde( Serdes.String() )
                                                          .withValueSerde( valueSerde ) )
                                           .suppress( Suppressed.untilTimeLimit( Duration.ofMinutes( getWindowLength() ),
                                                                                 Suppressed.BufferConfig.unbounded() ) )
                                           .toStream();

        groupedStream.to( "asset-candle.grouped", Produced.with( windowSerde, valueSerde ) );
        groupedStream.print( Printed.<Windowed<String>, AssetCandleTableMessageValue>toSysOut().withLabel( "Agg serde out" ) );
        //TODO: aggregate values of ktable tumbling window
        //TODO: ouput ktable to original stream
        //        groupedStream.map( keyValueMapper )
        //                     .to( SourceAssetCandleStream.SOURCE_TOPIC, Produced.with( new JsonSerde<>( AssetCandleMessageKey.class ),
        //                                                                               new JsonSerde<>( AssetCandleMessageValue.class ) ) );
    }


    private String getKey( final AssetCandleCompactedMessageKey key,
                           final AssetCandleTableMessageValue value )
    {
        final var minutesInDay = ( ( value.getTimestamp().getHour() * 60 ) + value.getTimestamp().getMinute() );
        final var periodOfDay = minutesInDay % getWindowLength() == 0 ?
                                minutesInDay / getWindowLength() -1 :
                                ( ( value.getTimestamp().getHour() * 60 ) + value.getTimestamp().getMinute() ) / getWindowLength();
        final var composedKey = key.getSymbol() + periodOfDay;
        log.info( "Composed key: " + composedKey );
        return composedKey;
    }


    private final KeyValueMapper<Windowed<String>, AssetCandleTableMessageValue, KeyValue<AssetCandleMessageKey, AssetCandleMessageValue>> keyValueMapper =
          ( key, value ) -> new KeyValue<>( AssetCandleMessageKey.builder()
                                                                 .timestamp( value.getTimestamp() )
                                                                 .interval( value.getInterval() )
                                                                 .symbol( key.key() ).build(),
                                            AssetCandleMessageValue.builder()
                                                                   .vwap( value.getVwap() )
                                                                   .close( value.getClose() )
                                                                   .open( value.getOpen() )
                                                                   .high( value.getHigh() )
                                                                   .low( value.getLow() )
                                                                   .volume( value.getVolume() )
                                                                   .build() );
}
