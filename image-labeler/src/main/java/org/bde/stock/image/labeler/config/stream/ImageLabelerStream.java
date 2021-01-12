package org.bde.stock.image.labeler.config.stream;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.kstream.WindowedSerdes;
import org.apache.kafka.streams.state.WindowStore;
import org.bde.stock.common.kafka.serde.AssetCandleAggregateDeserializer;
import org.bde.stock.common.kafka.serde.AssetCandleAggregator;
import org.bde.stock.common.kafka.serde.AssetCandleAggregatorSerializer;
import org.bde.stock.common.message.AssetCandleMessageValue;
import org.bde.stock.image.labeler.serde.AssetCandleWindowAggregator;
import org.bde.stock.image.labeler.serde.AssetCandleWindowAggregatorDeserializer;
import org.bde.stock.image.labeler.serde.AssetCandleWindowAggregatorSerializer;
import org.bde.stock.image.labeler.service.ImageLabelerService;
import org.bde.stock.image.labeler.util.TimestampExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Collections;
import java.util.List;


@Slf4j
@Configuration
public class ImageLabelerStream
{

    @Value( "${org.bde.stock.image.labeler.window-length.minutes}" )
    private Integer windowLength;

    //    private static final Pattern TOPIC_PATTERN = Pattern.compile( "(asset-candle.window).+" );

    @Value( "${org.bde.stock.image.labeler.topic}" )
    private String topic;

    @Value( "${org.bde.stock.image.labeler.look-forward.minutes}" )
    private Integer lookForwardMinutes;

    @Value( "${org.bde.stock.image.labeler.candle-interval.minutes}" )
    private Integer candleInterval;

    @Autowired
    private ImageLabelerService imageLabelerService;


    @Bean( name = "imageLabelStream" )
    public KStream<Windowed<String>, AssetCandleAggregator> kstreamAssetCandle( final StreamsBuilder builder )
    {
        final var windowTimeLength = Duration.ofMinutes( windowLength );
        final var keySerde = WindowedSerdes.timeWindowedSerdeFrom( String.class, windowTimeLength.toMillis() );
        keySerde.configure( Collections.emptyMap(), true );

        final var valueSerde = Serdes.serdeFrom( new AssetCandleAggregatorSerializer(), new AssetCandleAggregateDeserializer() );
        final var aggSerde = Serdes.serdeFrom( new AssetCandleWindowAggregatorSerializer(), new AssetCandleWindowAggregatorDeserializer() );


        // source stream
        KStream<Windowed<String>, AssetCandleAggregator> sourceStream = builder.stream( topic,
                                                                                        Consumed.with( keySerde, valueSerde, new TimestampExtractor(), null ) );

        final var adjacentWindowStream = sourceStream.filter( ( key, value ) -> fullWindow( value ) )
                                                     .groupBy( keyValueMapper,
                                                               Grouped.with( Serdes.String(), valueSerde ) )
                                                     .windowedBy( TimeWindows.of( Duration.ofMinutes( lookForwardMinutes + candleInterval ) )
                                                                             .advanceBy( Duration.ofMinutes( candleInterval ) )
                                                                             .grace( Duration.ofMillis( 1000 ) ) )
                                                     .aggregate( AssetCandleWindowAggregator::new,
                                                                 ( key, value, aggregate ) -> {
                                                                     aggregate.add( value );
                                                                     return aggregate;
                                                                 },
                                                                 Materialized.<String, AssetCandleWindowAggregator, WindowStore<Bytes, byte[]>>
                                                                       as( "ImageLabelStore" )
                                                                       .withKeySerde( Serdes.String() )
                                                                       .withValueSerde( aggSerde ) )
                                                     .toStream();
        adjacentWindowStream.filter( ( key, value ) -> containsLookahead( value ) )
                            .peek( ( key, value ) -> imageLabelerService.generateImageAndPublish( key.key(), value ) );

        return sourceStream;
    }


    /**
     * Make sure we are evaluating a full window.
     * If you have 5 minute candles and a window of 3 candles, the timestamps will be something like 12:00, 12:05, 12:10. (diff = 10 minutes)
     * The duration difference between them will be the window length - candleInterval
     */
    private boolean fullWindow( final AssetCandleAggregator value )
    {
        return Duration.between( value.getAgg().get( 0 ).getTimestamp(), value.getAgg().get( value.getAgg().size() - 1 ).getTimestamp() ).toMinutes() >= windowLength - candleInterval;
    }


    /**
     * Does this contain the lookahead candle needed to make a label determination.
     */
    private boolean containsLookahead( final AssetCandleWindowAggregator agg )
    {
        return Duration.between( agg.getWindowAgg().get( 0 ).getAgg().get( agg.getWindowAgg().get( 0 ).getAgg().size() - 1 ).getTimestamp(),
                                 agg.getWindowAgg().get( agg.getWindowAgg().size() - 1 ).getAgg().get( agg.getWindowAgg().get( agg.getWindowAgg().size() - 1 ).getAgg().size() - 1 ).getTimestamp() ).toMinutes()
               == lookForwardMinutes;
    }


    /**
     * Wanted to make this a generic stream for image labeling but because we need to define timeframes it cannot be dynamic.
     */
    private KeyValueMapper<Windowed<String>, AssetCandleAggregator, String> keyValueMapper = ( key, value ) -> key.key();
}
