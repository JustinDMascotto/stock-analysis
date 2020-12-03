package org.bde.stock.ingestion.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetCandleMessageValue
{
    private double open;

    private double close;

    private double high;

    private double low;

    private Double vwap;

    private int volume;
}
