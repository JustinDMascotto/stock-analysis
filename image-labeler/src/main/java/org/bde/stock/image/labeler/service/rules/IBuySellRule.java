package org.bde.stock.image.labeler.service.rules;

import org.bde.stock.common.kafka.serde.AssetCandleAggregator;
import org.bde.stock.common.message.AssetCandleMessageValue;
import org.bde.stock.image.labeler.domain.Determination;

import java.util.List;


public interface IBuySellRule
{
    boolean matchesInterval( Integer interval );

    AssetCandleMessageValue getDeterminationCandle( AssetCandleAggregator windowToLabel,
                                                    List<AssetCandleAggregator> windows );

    Determination determineBuySell( List<AssetCandleMessageValue> window,
                                    AssetCandleMessageValue determinationCandle );
}
