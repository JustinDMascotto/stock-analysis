package org.bde.stock.image.labeler.serde;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;


@NoArgsConstructor
public class AssetCandleWindowAggregatorSerializer
      implements Serializer<AssetCandleWindowAggregator>
{
    @Override
    public void configure( Map<String, ?> configs,
                           boolean isKey )
    {
        // do nothing
    }

    @Override
    public byte[] serialize( final String topic,
                             final AssetCandleWindowAggregator data )
    {
        if ( data == null )
        {
            return null;
        }

        try
        {
            return data.asByteArray();
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
