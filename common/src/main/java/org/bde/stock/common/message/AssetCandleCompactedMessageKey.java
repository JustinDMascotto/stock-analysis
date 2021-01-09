package org.bde.stock.common.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetCandleCompactedMessageKey
{
    private String symbol;

    private int interval;

    private int minuteOfTheDay;
}
