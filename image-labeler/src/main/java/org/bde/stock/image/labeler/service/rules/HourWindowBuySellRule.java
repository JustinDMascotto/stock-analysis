package org.bde.stock.image.labeler.service.rules;

import org.bde.stock.common.kafka.serde.AssetCandleAggregator;
import org.bde.stock.common.message.AssetCandleMessageValue;
import org.bde.stock.image.labeler.domain.Determination;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;


@Component
public class HourWindowBuySellRule
      implements IBuySellRule
{

    @Value( "${org.bde.stock.image.labeler.look-forward.minutes}" )
    private Integer lookForwardMinutes;


    @Override
    public boolean matchesInterval( final Integer interval )
    {
        return interval == 60;
    }


    @Override
    public AssetCandleMessageValue getDeterminationCandle( final AssetCandleAggregator windowToLabel,
                                                           final List<AssetCandleAggregator> windows )
    {
        return windows.get( windows.size() - 1 ).getAgg().stream().filter( candle -> Duration.between( windowToLabel.getAgg().get( windowToLabel.getAgg().size() - 1 ).getTimestamp(), candle.getTimestamp() ).toMinutes() == lookForwardMinutes ).findFirst()
                      .orElseThrow( () -> new IllegalStateException( "No window with lookahead determination candle. Check stream config." ) );
    }


    @Override
    public Determination determineBuySell( final List<AssetCandleMessageValue> window,
                                           final AssetCandleMessageValue determinationCandle )
    {
        final Determination determination;
        final var diff = determinationCandle.getHigh() - window.get( window.size() -1 ).getClose();

        // if diff between determination and window is above .5% BUY
        if ( diff > 0 &&
             diff / window.get( window.size()-1 ).getClose() > 0.01 )
        {
            return Determination.BUY;
        }
        else if ( diff < 0 &&
                  diff / window.get( window.size()-1 ).getClose() < -0.01 )
        {
            return Determination.SELL;
        }

        return Determination.DO_NOTHING;
    }
}
