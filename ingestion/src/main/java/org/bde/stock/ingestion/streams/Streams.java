package org.bde.stock.ingestion.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Aggregator;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.ForeachAction;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Reducer;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Window;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.kstream.WindowedSerdes;
import org.apache.kafka.streams.state.WindowStore;
import org.bde.stock.ingestion.message.AssetCandleCompactedMessageKey;
import org.bde.stock.ingestion.message.AssetCandleMessageKey;
import org.bde.stock.ingestion.message.AssetCandleMessageValue;
import org.bde.stock.ingestion.model.AssetCandle;
import org.bde.stock.ingestion.streams.serde.AssetCandleAggregateDeserializer;
import org.bde.stock.ingestion.streams.serde.AssetCandleAggregator;
import org.bde.stock.ingestion.streams.serde.AssetCandleAggregatorSerializer;
import org.bde.stock.ingestion.util.TimestampExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;


@Configuration
public class Streams
{
    @Bean
    public KStream<AssetCandleMessageKey, AssetCandleMessageValue> kstreamAssetCandle( final StreamsBuilder builder )
    {
        final var keySerde = new JsonSerde<>( AssetCandleMessageKey.class );
        final var valueSerde = new JsonSerde<>( AssetCandleMessageValue.class );

        final var compactedKeySerde = new JsonSerde<>( AssetCandleCompactedMessageKey.class );

        // source stream
        KStream<AssetCandleMessageKey, AssetCandleMessageValue> sourceStream = builder.stream( "asset-candle",
                                                                                               Consumed.with( keySerde, valueSerde, new TimestampExtractor(), null ) );

        // compacted topic stream
        KStream<AssetCandleCompactedMessageKey, AssetCandleMessageValue> compacted = sourceStream.map( keyValueMapper );
        compacted.to( "asset-candle.compacted", Produced.with( compactedKeySerde, valueSerde ) );


        final var hopLength = Duration.ofMinutes( 1 );
        final var windowLength = Duration.ofMinutes( 5 );
        final var aggSerde = Serdes.serdeFrom( new AssetCandleAggregatorSerializer(), new AssetCandleAggregateDeserializer() );

        final var windowSerde = WindowedSerdes.timeWindowedSerdeFrom( String.class, windowLength.toMillis() );
        final var groupedStream = compacted.groupBy( ( key, value ) -> key.getSymbol(),
                                                     Grouped.with( Serdes.String(), valueSerde ) )
                                           .windowedBy( TimeWindows.of( windowLength ).advanceBy( hopLength ) )
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

        groupedStream.to( "asset-candle.grouped", Produced.with( windowSerde, aggSerde ) );

        sourceStream.print( Printed.<AssetCandleMessageKey, AssetCandleMessageValue>toSysOut().withLabel( "Json serde original stream " ) );
        compacted.print( Printed.<AssetCandleCompactedMessageKey, AssetCandleMessageValue>toSysOut().withLabel( "Json serde compacted stream " ) );
        groupedStream.print( Printed.<Windowed<String>, AssetCandleAggregator>toSysOut().withLabel( "Agg serde out" ) );

        return sourceStream;
    }


    private final KeyValueMapper<AssetCandleMessageKey, AssetCandleMessageValue, KeyValue<? extends AssetCandleCompactedMessageKey, ? extends AssetCandleMessageValue>> keyValueMapper =
          ( key, value ) -> {
              final var startOfDay = LocalDate.now( ZoneOffset.UTC ).atStartOfDay();
              final var minutesToday = Duration.between( startOfDay, LocalDateTime.now( ZoneOffset.UTC ) ).toMinutes();
              return new KeyValue<>( AssetCandleCompactedMessageKey.builder()
                                                                   .interval( key.getInterval() )
                                                                   .symbol( key.getSymbol() )
                                                                   .minuteOfTheDay( (int) minutesToday )
                                                                   .build(),
                                     value );
          };
}
