package org.bde.stock.ingestion.serde;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bde.stock.ingestion.message.AssetCandleMessageValue;
import org.bde.stock.ingestion.model.AssetCandle;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetCandleAggregator
{
    private ArrayList<AssetCandleMessageValue> agg = new ArrayList<>();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static
    {
        MAPPER.registerModule( new JavaTimeModule() );
    }


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

    public AssetCandleAggregator addAll( Collection<AssetCandleMessageValue> list )
    {
        agg.addAll( list );
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
