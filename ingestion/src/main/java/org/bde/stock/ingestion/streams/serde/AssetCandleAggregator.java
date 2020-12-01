package org.bde.stock.ingestion.streams.serde;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bde.stock.ingestion.message.AssetCandleMessageValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class AssetCandleAggregator
{
    private ArrayList<AssetCandleMessageValue> agg = new ArrayList<>();

    private static final ObjectMapper MAPPER = new ObjectMapper();


    public AssetCandleAggregator( String jsonString )
          throws Exception
    {
        List<AssetCandleMessageValue> logEntries = MAPPER.readValue( jsonString, new TypeReference<List<AssetCandleMessageValue>>() {} );
        agg.addAll( logEntries );
    }


    public AssetCandleAggregator( byte[] bytes )
          throws Exception
    {
        this( new String( bytes ) );
    }


    public AssetCandleAggregator add( String log )
          throws Exception
    {
        AssetCandleMessageValue logEntry = MAPPER.readValue( log, AssetCandleMessageValue.class );
        agg.add( logEntry );
        return this;
    }

    public AssetCandleAggregator add( AssetCandleMessageValue log )
    {
        agg.add( log );
        return this;
    }


    public String asJsonString()
          throws Exception
    {
        return MAPPER.writeValueAsString( agg );
    }


    public byte[] asByteArray()
          throws Exception
    {
        return asJsonString().getBytes( StandardCharsets.UTF_8 );
    }
}
