package org.bde.stock.ingestion.config.streams.aggregate;

import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Printed;
import org.bde.stock.ingestion.message.AssetCandleMessageKey;
import org.bde.stock.ingestion.message.AssetCandleMessageValue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;


@Configuration
public class FiveMinuteAssetCandleAggregator
      extends AbstractAssetCandleAggregator
{
    public static final String outputTopic = "asset-candle.five";

    @Bean( name = "fiveMinuteCandles" )
    public KStream<AssetCandleMessageKey, AssetCandleMessageValue> fiveMinuteCandleStream( @Qualifier( "singleMinuteCandles" ) final KStream<AssetCandleMessageKey, AssetCandleMessageValue> singleMinuteCandles )
    {
        final var fiveMinuteCandleStream = this.kstreamAssetCandleAgg( singleMinuteCandles, outputTopic );
        fiveMinuteCandleStream.print( Printed.<AssetCandleMessageKey, AssetCandleMessageValue>toSysOut().withLabel( "5 minute candle: " ) );
        return fiveMinuteCandleStream;
    }


    @Override
    Duration getWindowLength()
    {
        return Duration.ofMinutes( 5 );
    }


    @Override
    Integer getCompositeInterval()
    {
        return 1;
    }
}
