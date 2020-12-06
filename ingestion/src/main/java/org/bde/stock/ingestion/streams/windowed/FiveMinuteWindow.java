package org.bde.stock.ingestion.streams.windowed;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.kstream.WindowedSerdes;
import org.apache.kafka.streams.state.WindowStore;
import org.bde.stock.ingestion.message.AssetCandleMessageKey;
import org.bde.stock.ingestion.message.AssetCandleMessageValue;
import org.bde.stock.ingestion.serde.AssetCandleAggregateDeserializer;
import org.bde.stock.ingestion.serde.AssetCandleAggregator;
import org.bde.stock.ingestion.serde.AssetCandleAggregatorSerializer;
import org.bde.stock.ingestion.streams.aggregate.FiveMinuteAssetCandleAggregator;
import org.bde.stock.ingestion.util.TimestampExtractor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;


@Configuration
public class FiveMinuteWindow
{

    @Bean
    public KStream<Windowed<String>, AssetCandleAggregator> fifteenMinuteWindow( final StreamsBuilder builder )
    {
        final var hopLength = Duration.ofMinutes( 1 );
        final var windowLength = Duration.ofMinutes( 5 );
        final var aggSerde = Serdes.serdeFrom( new AssetCandleAggregatorSerializer(), new AssetCandleAggregateDeserializer() );
        final var keySerde = new JsonSerde<>( AssetCandleMessageKey.class );
        final var valueSerde = new JsonSerde<>( AssetCandleMessageValue.class );

        final var windowSerde = WindowedSerdes.timeWindowedSerdeFrom( String.class, windowLength.toMillis() );
        final var windowed = builder.stream( FiveMinuteAssetCandleAggregator.outputTopic,
                                             Consumed.with( keySerde, valueSerde, new TimestampExtractor(), null ) )
                                    .filter( ( key, value ) -> key.getInterval() == 5 )
                                    .groupBy( ( key, value ) -> key.getSymbol(),
                                              Grouped.with( Serdes.String(), valueSerde ) )
                                    .windowedBy( TimeWindows.of( windowLength )
                                                            .advanceBy( hopLength )
                                                            .grace( Duration.ofMillis( 1000 ) ) )
                                    .aggregate( AssetCandleAggregator::new,
                                                ( key, value, aggregate ) -> {
                                                    aggregate.add( value );
                                                    return aggregate;
                                                },
                                                Materialized.<String, AssetCandleAggregator, WindowStore<Bytes, byte[]>>
                                                      as( "store" )
                                                      .withKeySerde( Serdes.String() )
                                                      .withValueSerde( aggSerde ) )
                                    .toStream();

        windowed.to( "asset-candle.five.window", Produced.with( windowSerde, aggSerde ) );
        windowed.print( Printed.<Windowed<String>, AssetCandleAggregator>toSysOut().withLabel( "Window: " ) );

        return windowed;
//        return null;
    }
}
