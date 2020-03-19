package org.bde.chart.generator.service.ruleengines;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bde.chart.generator.entity.StockCandleEntity;
import org.bde.chart.generator.repository.StockCandleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import java.time.Duration;



@Slf4j
@Component
public class FifteenMinuteVolumeRuleEngine
      extends AbstractBuySellRuleEngine
{
    private static final Duration VOLUME_HISTORY_WINDOW_DURATION = Duration.ofHours( 18 );

    @Getter
    private final String outputDir;

    private final StockCandleRepository stockCandleRepository;


    public FifteenMinuteVolumeRuleEngine( @Value( "${bde.stock-analysis.image-generator.output-dir}" ) final String outputDir,
                                          final StockCandleRepository stockCandleRepository )
    {
        super( stockCandleRepository, outputDir );
        this.outputDir = outputDir;
        this.stockCandleRepository = stockCandleRepository;
    }


    @Override
    protected RuleEngineOutput determineBuyOrSell( final StockCandleEntity currentClosedCandle,
                                                   final StockCandleEntity previousCandle,
                                                   final StockCandleEntity lookAheadCandle )
    {
        final double risk = calculateRisk( currentClosedCandle, previousCandle, lookAheadCandle );
        if ( enoughReward( currentClosedCandle, lookAheadCandle, risk ) &&
             enoughConfirmation( currentClosedCandle ) )
        {
            if ( bullish( currentClosedCandle, lookAheadCandle ) )
            {
                return RuleEngineOutput.BUY;
            }
            else
            {
                return RuleEngineOutput.SELL;
            }
        }

        return RuleEngineOutput.DO_NOTHING;
    }


    /**
     * A setup is considered bullish if the next candle close is higher then the previous candle close.
     */
    private boolean bullish( final StockCandleEntity currentClosedCandle,
                             final StockCandleEntity lookAheadCandle )
    {
        return currentClosedCandle.getClose() > lookAheadCandle.getClose();
    }


    private boolean enoughReward( final StockCandleEntity currentCandle,
                                  final StockCandleEntity lookAheadCandle,
                                  final double risk )
    {
        return Math.abs( risk ) > 1 &&
               Math.abs( lookAheadCandle.getClose() - currentCandle.getClose() ) > Math.abs( risk ) * 3;
    }


    private boolean enoughConfirmation( final StockCandleEntity currentCandle )
    {
        final double averageVolume = stockCandleRepository.meanVolumeBetweenDates( currentCandle.getTimestamp().minus( VOLUME_HISTORY_WINDOW_DURATION ),
                                                                                   currentCandle.getTimestamp(),
                                                                                   currentCandle.getTicker().getName() );

        final double slope = stockCandleRepository.slope( currentCandle.getTimestamp().minus( getCandleWindowDuration() ),
                                                          currentCandle.getTimestamp(),
                                                          currentCandle.getTicker().getId() );

        return averageVolume * 2.5 < currentCandle.getVolume() &&
               Math.abs( slope ) > 0.6;
    }


    /**
     * Risk will be positive if bullish or negative if bearish.
     */
    private double calculateRisk( final StockCandleEntity currentClosedCandle,
                                  final StockCandleEntity previousCandle,
                                  final StockCandleEntity lookAheadCandle )
    {
        final double risk;
        if ( bullish( currentClosedCandle, lookAheadCandle ) )
        {
            risk = currentClosedCandle.getClose() - previousCandle.getLow();
        }
        else
        {
            risk = currentClosedCandle.getClose() - previousCandle.getHigh();
        }

        return risk;
    }


    @Override
    protected Duration getLookAheadDuration()
    {
        return Duration.ofMinutes( 15 );
    }


    @Override
    public Duration getCandleWindowDuration()
    {
        return Duration.ofMinutes( 360 );
    }


    @Override
    protected Integer getOutputInterval()
    {
        return 15;
    }
}
