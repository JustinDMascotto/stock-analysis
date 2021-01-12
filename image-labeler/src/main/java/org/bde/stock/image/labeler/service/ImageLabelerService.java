package org.bde.stock.image.labeler.service;

import org.bde.stock.common.message.AssetCandleMessageValue;
import org.bde.stock.common.render.CandlestickChart;
import org.bde.stock.common.render.ImageContainer;
import org.bde.stock.image.labeler.domain.Determination;
import org.bde.stock.image.labeler.serde.AssetCandleWindowAggregator;
import org.bde.stock.image.labeler.service.rules.IBuySellRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;


@Service
public class ImageLabelerService
{

    @Autowired
    private List<IBuySellRule> buySellRules;


    public void generateImageAndPublish( final String symbol,
                                         final AssetCandleWindowAggregator assetCandleWindowAggregator )
    {
        final var windowToLabel = assetCandleWindowAggregator.getWindowAgg().get( 0 );
        final var candleIntervalInMinutes = windowToLabel.getAgg().get( 0 ).getInterval();
        final var buySellRule = buySellRules.stream().filter( rule -> rule.matchesInterval( candleIntervalInMinutes ) )
                                            .findAny().orElseThrow( () -> new IllegalStateException( "No buy or sell rules for " ) );
        final var determinationCandle = buySellRule.getDeterminationCandle( windowToLabel, assetCandleWindowAggregator.getWindowAgg() );
        final var determination = buySellRule.determineBuySell( windowToLabel.getAgg(), determinationCandle );
        generateGraph( symbol, windowToLabel.getAgg(), determination );
    }


    private void generateGraph( final String symbol,
                                final List<AssetCandleMessageValue> candles,
                                final Determination determination )
    {
        final CandlestickChart chart = new CandlestickChart();
        IntStream.range( 1, candles.size() + 1 )
                 .forEach( i -> {
                     final var candle = candles.get( i - 1 );
                     chart.addCandle( candle, (long) i );
                 } );
        //                    chart.setBorder( null );
        //                    chart.setForeground( Color.WHITE );
        ImageContainer.toImage( new ImageContainer( Arrays.asList( chart ) ).getContentPane(), "/Users/justinmascotto/Downloads/images/" + symbol + "/" + determination + "/" + symbol +"_" + candles.get( candles.size() - 1 ).getTimestamp().format( DateTimeFormatter.ofPattern( "yyyy.MM.dd'T'HH.mm.ss" ) ) + ".png" );
    }
}
