package org.bde.stock.ingestion.config.streams.windowed;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.kstream.ForeachAction;
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
import org.bde.stock.ingestion.util.CandlestickChart;
import org.bde.stock.ingestion.util.ImageContainer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.awt.Color;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.IntStream;


@Configuration
public class FiveMinuteWindow
{

    @Bean
    public KStream<Windowed<String>, AssetCandleAggregator> fifteenMinuteWindow( @Qualifier( "fiveMinuteCandles") final KStream<AssetCandleMessageKey,AssetCandleMessageValue> fiveMinuteStream )
    {
        final var candleSize = Duration.ofMinutes( 5 );
        final var windowTimeLength = Duration.ofMinutes( 25 );
        final var aggSerde = Serdes.serdeFrom( new AssetCandleAggregatorSerializer(), new AssetCandleAggregateDeserializer() );
        final var valueSerde = new JsonSerde<>( AssetCandleMessageValue.class );

        final var windowSerde = WindowedSerdes.timeWindowedSerdeFrom( String.class, windowTimeLength.toMillis() );
        final var windowed = fiveMinuteStream
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
                                                      as( "store" )
                                                      .withKeySerde( Serdes.String() )
                                                      .withValueSerde( aggSerde ) )
                                    .toStream();

        windowed.to( "asset-candle.five.window", Produced.with( windowSerde, aggSerde ) );
        windowed.print( Printed.<Windowed<String>, AssetCandleAggregator>toSysOut().withLabel( "Window: " ) );
        windowed.peek( ( key, value ) -> {
            if ( value.getAgg().size() == 5 )
            {
                final CandlestickChart chart = new CandlestickChart();
                IntStream.range( 1, value.getAgg().size() + 1 )
                         .forEach( i -> {
                             final var candle = value.getAgg().get( i - 1 );
                             chart.addCandle( candle, (long) i );
                         } );
//                    chart.setBorder( null );
//                    chart.setForeground( Color.WHITE );
                ImageContainer.toImage( new ImageContainer( Arrays.asList( chart ) ).getContentPane(),"/Users/justinmascotto/Downloads/images/" + key.key() + "_" + value.getAgg().get( value.getAgg().size() - 1 ).getTimestamp().format( DateTimeFormatter.ofPattern( "yyyy.MM.dd'T'HH.mm.ss" )  ) + ".png" );
            }
        } );
        return windowed;
    }
}
