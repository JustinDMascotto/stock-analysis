package org.bde.stock.image.labeler.serde;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;


@NoArgsConstructor
public class AssetCandleWindowAggregatorDeserializer
      implements Deserializer<AssetCandleWindowAggregator>
{
    @Override
    public void configure( final Map<String, ?> configs,
                           final boolean isKey )
    {
        // do nothing
    }


    @Override
    public AssetCandleWindowAggregator deserialize( final String topic,
                                                    final byte[] data )
    {
        if ( data == null )
        {
            return null;
        }

        try
        {
            return new AssetCandleWindowAggregator( data );
        }
        catch ( Exception e )
        {
            throw new SerializationException( "Error deserializing value", e );
        }
    }


    @Override
    public void close()
    {

    }
}
