package org.bde.stock.ingestion.streams.serde;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

@NoArgsConstructor
public class AssetCandleAggregateDeserializer
      implements Deserializer<AssetCandleAggregator>
{
    @Override
    public void configure( Map<String, ?> configs,
                           boolean isKey )
    {
        // do nothing
    }


    @Override
    public AssetCandleAggregator deserialize( String topic,
                                              byte[] bytes )
    {
        if ( bytes == null )
        {
            return null;
        }

        try
        {
            return new AssetCandleAggregator( bytes );
        }
        catch ( Exception e )
        {
            throw new SerializationException( "Error deserializing value", e );
        }
    }


    @Override
    public void close()
    {
        // do nothing
    }
}
