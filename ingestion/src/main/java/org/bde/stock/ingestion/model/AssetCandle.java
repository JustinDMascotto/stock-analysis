package org.bde.stock.ingestion.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetCandle
{
    private String ticker;

    private Double open;

    private Double close;

    private Double high;

    private Double low;

    private Double vwap;

    private Integer volume;

    private Integer interval;

    private LocalDateTime timestamp;
}
