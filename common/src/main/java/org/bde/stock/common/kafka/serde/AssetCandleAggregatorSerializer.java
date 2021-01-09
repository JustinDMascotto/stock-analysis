package org.bde.stock.common.kafka.serde;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;


@NoArgsConstructor
public class AssetCandleAggregatorSerializer
      implements Serializer<AssetCandleAggregator>
{

    @Override
    public void configure( Map<String, ?> configs,
                           boolean isKey )
    {
        // do nothing
    }


    @Override
    public byte[] serialize( String topic,
                             AssetCandleAggregator queue )
    {
        if ( queue == null )
        {
            return null;
        }

        try
        {
            return queue.asByteArray();
        }
        catch ( Exception e )
        {
            throw new SerializationException( "Error serializing value", e );
        }
    }


    @Override
    public void close()
    {
    }
}
