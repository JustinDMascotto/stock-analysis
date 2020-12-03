package org.bde.stock.ingestion.streams.aggregate;

import org.apache.kafka.streams.kstream.KStream;
import org.bde.stock.ingestion.message.AssetCandleCompactedMessageKey;
import org.bde.stock.ingestion.message.AssetCandleTableMessageValue;
import org.springframework.stereotype.Component;


@Component
public class FiveMinuteAssetCandleAggregator
    extends AbstractAssetCandleAggregator
{

    FiveMinuteAssetCandleAggregator( final KStream<AssetCandleCompactedMessageKey, AssetCandleTableMessageValue> compacted )
    {
        this.kstreamAssetCandleAgg( compacted );
    }

    @Override
    Integer getWindowLength()
    {
        return 5;
    }


    @Override
    Integer getCompositeInterval()
    {
        return 1;
    }
}
