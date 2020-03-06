package org.bde.chart.generator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Candle
{
    private long timestamp;

    private int interval;

    private double open;

    private double close;

    private double high;

    private double low;

    private double vwap;

    private int volume;
}
