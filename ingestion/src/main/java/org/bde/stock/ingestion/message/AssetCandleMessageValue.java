package org.bde.stock.ingestion.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


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

    @JsonFormat( pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;

    private int interval;
}
