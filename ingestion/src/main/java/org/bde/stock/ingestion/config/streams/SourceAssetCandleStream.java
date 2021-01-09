package org.bde.stock.ingestion.config.streams;


import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Printed;
import org.bde.stock.common.message.AssetCandleMessageKey;
import org.bde.stock.common.message.AssetCandleMessageValue;
import org.bde.stock.ingestion.util.TimestampExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.Collections;



@Configuration
public class SourceAssetCandleStream
{
    public static final String SOURCE_TOPIC = "asset-candle";

    @Bean( name = "singleMinuteCandles")
    public KStream<AssetCandleMessageKey, AssetCandleMessageValue> kstreamAssetCandle( final StreamsBuilder builder )
    {
        final var keySerde = new JsonSerde<>( AssetCandleMessageKey.class );
        keySerde.configure( Collections.emptyMap(), true );
        final var valueSerde = new JsonSerde<>( AssetCandleMessageValue.class );
        valueSerde.configure( Collections.emptyMap(), false );

        // source stream
        KStream<AssetCandleMessageKey, AssetCandleMessageValue> sourceStream = builder.stream( SOURCE_TOPIC,
                                                                                               Consumed.with( keySerde, valueSerde, new TimestampExtractor(), null ) );

        sourceStream.print( Printed.<AssetCandleMessageKey, AssetCandleMessageValue>toSysOut().withLabel( "Json serde original stream " ) );

        return sourceStream;
    }
}
