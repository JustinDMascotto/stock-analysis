package org.bde.stock.ingestion.streams;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Predicate;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.kstream.WindowedSerdes;
import org.apache.kafka.streams.state.WindowStore;
import org.bde.stock.ingestion.message.AssetCandleCompactedMessageKey;
import org.bde.stock.ingestion.message.AssetCandleMessageKey;
import org.bde.stock.ingestion.message.AssetCandleMessageValue;
import org.bde.stock.ingestion.message.AssetCandleTableMessageValue;
import org.bde.stock.ingestion.serde.AssetCandleAggregateDeserializer;
import org.bde.stock.ingestion.serde.AssetCandleAggregator;
import org.bde.stock.ingestion.serde.AssetCandleAggregatorSerializer;
import org.bde.stock.ingestion.util.TimestampExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;



@Configuration
public class SourceAssetCandleStream
{
    public static final String SOURCE_TOPIC = "asset-candle";

    public static final String COMPACTED_TOPIC = "asset-candle.compacted";

    @Bean( name = "singleMinuteCandles")
    public KStream<AssetCandleMessageKey, AssetCandleMessageValue> kstreamAssetCandle( final StreamsBuilder builder )
    {
        final var keySerde = new JsonSerde<>( AssetCandleMessageKey.class );
        final var valueSerde = new JsonSerde<>( AssetCandleMessageValue.class );

        // source stream
        KStream<AssetCandleMessageKey, AssetCandleMessageValue> sourceStream = builder.stream( SOURCE_TOPIC,
                                                                                               Consumed.with( keySerde, valueSerde, new TimestampExtractor(), null ) );

//        // compacted topic stream
//        KStream<AssetCandleCompactedMessageKey, AssetCandleMessageValue> compacted = sourceStream.map( keyValueMapper );
//        compacted.to( "asset-candle.compacted", Produced.with( compactedKeySerde, valueSerde ) );
//
//


        sourceStream.print( Printed.<AssetCandleMessageKey, AssetCandleMessageValue>toSysOut().withLabel( "Json serde original stream " ) );

        return sourceStream;
    }
}
