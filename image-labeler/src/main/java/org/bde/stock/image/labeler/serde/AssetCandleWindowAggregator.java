package org.bde.stock.image.labeler.serde;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bde.stock.common.kafka.serde.AssetCandleAggregator;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetCandleWindowAggregator
{
    private ArrayList<AssetCandleAggregator> windowAgg = new ArrayList<>();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static
    {
        MAPPER.registerModule( new JavaTimeModule() );
    }

    public AssetCandleWindowAggregator( final String jsonString )
          throws Exception
    {
        final var entries = MAPPER.readValue( jsonString, new TypeReference<List<AssetCandleAggregator>>() {} );
        windowAgg.addAll( entries );
    }

    public AssetCandleWindowAggregator( byte[] bytes )
          throws Exception
    {
        this( new String( bytes ) );
    }

    public AssetCandleWindowAggregator add( AssetCandleAggregator agg )
    {
        windowAgg.add( agg );
        return this;
    }

    public String asJsonString()
          throws Exception
    {
        return MAPPER.writeValueAsString( windowAgg );
    }

    public byte[] asByteArray()
          throws Exception
    {
        return asJsonString().getBytes( StandardCharsets.UTF_8 );
    }
}
