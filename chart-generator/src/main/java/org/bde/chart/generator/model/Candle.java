package org.bde.chart.generator.model;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class Candle
{
    private long timestamp;

    private int interval;

    private double open;

    private double close;

    private double high;

    private double low;

    private int volume;
}
