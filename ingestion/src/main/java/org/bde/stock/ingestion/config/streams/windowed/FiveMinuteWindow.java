package org.bde.stock.ingestion.config.streams.windowed;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.kstream.WindowedSerdes;
import org.apache.kafka.streams.state.WindowStore;
import org.bde.stock.common.message.AssetCandleMessageKey;
import org.bde.stock.common.message.AssetCandleMessageValue;
import org.bde.stock.common.kafka.serde.AssetCandleAggregateDeserializer;
import org.bde.stock.common.kafka.serde.AssetCandleAggregator;
import org.bde.stock.common.kafka.serde.AssetCandleAggregatorSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;


@Configuration
public class FiveMinuteWindow
{

    @Value( "${org.bde.stock.window.five.length}" )
    private Integer windowLength;

    @Bean
    public KStream<Windowed<String>, AssetCandleAggregator> fifteenMinuteWindow( @Qualifier( "candleSource") final KStream<AssetCandleMessageKey,AssetCandleMessageValue> candleSource )
    {
        final var candleSize = Duration.ofMinutes( 5 );
        final var windowTimeLength = Duration.ofMinutes( windowLength );
        final var aggSerde = Serdes.serdeFrom( new AssetCandleAggregatorSerializer(), new AssetCandleAggregateDeserializer() );
        final var valueSerde = new JsonSerde<>( AssetCandleMessageValue.class );

        final var windowSerde = WindowedSerdes.timeWindowedSerdeFrom( String.class, windowTimeLength.toMillis() );
        final var windowed = candleSource
                                    .filter( ( key, value ) -> key.getInterval() == 5 )
                                    .groupBy( ( key, value ) -> key.getSymbol(),
                                              Grouped.with( Serdes.String(), valueSerde ) )
                                    .windowedBy( TimeWindows.of( windowTimeLength )
                                                            .advanceBy( candleSize )
                                                            .grace( Duration.ofMillis( 1000 ) ) )
                                    .aggregate( AssetCandleAggregator::new,
                                                ( key, value, aggregate ) -> {
                                                    aggregate.add( value );
                                                    return aggregate;
                                                },
                                                Materialized.<String, AssetCandleAggregator, WindowStore<Bytes, byte[]>>
                                                      as( "WindowedStoreFive" )
                                                      .withKeySerde( Serdes.String() )
                                                      .withValueSerde( aggSerde ) )
                                    .toStream();

        windowed.to( "asset-candle.window.five", Produced.with( windowSerde, aggSerde ) );
        windowed.print( Printed.<Windowed<String>, AssetCandleAggregator>toSysOut().withLabel( "Window 5 minute" ) );
        return windowed;
    }
}
