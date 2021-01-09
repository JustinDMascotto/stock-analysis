package org.bde.stock.common.message;

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

    private Double volume;

    @JsonFormat( pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;

    private int interval;
}
