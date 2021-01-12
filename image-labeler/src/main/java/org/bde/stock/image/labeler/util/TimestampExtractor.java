package org.bde.stock.image.labeler.util;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bde.stock.common.kafka.serde.AssetCandleAggregator;

import java.time.ZoneOffset;


public class TimestampExtractor
    implements org.apache.kafka.streams.processor.TimestampExtractor
{
    @Override
    public long extract( final ConsumerRecord<Object, Object> record,
                         final long partitionTime )
    {
        var window = ((AssetCandleAggregator) record.value()).getAgg();
        var latestCandleInWindow = window.get( window.size() -1 );
        return latestCandleInWindow.getTimestamp().toInstant( ZoneOffset.UTC ).toEpochMilli();
    }
}
