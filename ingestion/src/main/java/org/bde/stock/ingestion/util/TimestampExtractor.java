package org.bde.stock.ingestion.util;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bde.stock.ingestion.message.AssetCandleMessageKey;
import org.bde.stock.ingestion.message.AssetCandleMessageValue;

import java.time.ZoneOffset;


public class TimestampExtractor
      implements org.apache.kafka.streams.processor.TimestampExtractor
{
    @Override
    public long extract( final ConsumerRecord<Object, Object> record,
                         final long partitionTime )
    {
        var assetCandleTimestamp = (AssetCandleMessageValue) record.value();
        return assetCandleTimestamp != null ? assetCandleTimestamp.getTimestamp().toInstant( ZoneOffset.UTC ).toEpochMilli() : record.timestamp();
    }
}
