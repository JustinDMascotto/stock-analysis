package org.bde.chart.generator.render;

import lombok.Getter;
import org.bde.stock.common.message.AssetCandleMessageValue;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.ohlc.OHLCSeries;
import org.jfree.data.time.ohlc.OHLCSeriesCollection;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.text.DecimalFormat;


public class CandlestickChart
      extends JPanel
{
    private OHLCSeries ohlcSeries;

    private TimeSeries volumeSeries;

    @Getter
    private ChartPanel chartPanel;


    public CandlestickChart()
    {
        final JFreeChart candlestickChart = createChart();
        chartPanel = new ChartPanel( candlestickChart );
        chartPanel.setPreferredSize( new Dimension( 200, 250 ) );
        add( chartPanel, BorderLayout.CENTER );
    }


    private JFreeChart createChart()
    {
        /**
         * Creating candlestick subplot
         */
        // Create OHLCSeriesCollection as a price dataset for candlestick chart
        final OHLCSeriesCollection candlestickDataset = new OHLCSeriesCollection();
        ohlcSeries = new OHLCSeries( "Price" );
        candlestickDataset.addSeries( ohlcSeries );
        // Create candlestick chart priceAxis
        final NumberAxis priceAxis = new NumberAxis();
        priceAxis.setAutoRangeIncludesZero( false );
        priceAxis.setTickMarksVisible( false );
        priceAxis.setTickLabelsVisible( false );

        final CandlestickRenderer candlestickRenderer = new CandlestickRenderer( CandlestickRenderer.WIDTHMETHOD_AVERAGE );

        // Create candlestickSubplot
        final XYPlot candlestickSubplot = new XYPlot( candlestickDataset, null, priceAxis, candlestickRenderer );

        candlestickSubplot.setBackgroundPaint( Color.WHITE );
        candlestickSubplot.setRangeGridlinesVisible( false );
        candlestickSubplot.setDomainGridlinesVisible( false );
        candlestickSubplot.setOutlinePaint( null );

        /**
         * Creating volume subplot
         */
        // creates TimeSeriesCollection as a volume dataset for volume chart
        final TimeSeriesCollection volumeDataset = new TimeSeriesCollection();
        volumeSeries = new TimeSeries( "Volume" );
        volumeDataset.addSeries( volumeSeries );
        // Create volume chart volumeAxis
        final NumberAxis volumeAxis = new NumberAxis();
//        volumeAxis.setAutoRangeIncludesZero( false );

        // Set to no decimal
        volumeAxis.setNumberFormatOverride( new DecimalFormat( "0" ) );
//        volumeAxis.setTickMarksVisible( false );
//        volumeAxis.setTickLabelsVisible( false );

        // Create volume chart renderer
        XYBarRenderer timeRenderer = new XYBarRenderer();
//        timeRenderer.setShadowVisible( false );
        // Create volumeSubplot

        final ValueAxis axis = new NumberAxis();
        axis.setVisible( false );
        final XYPlot volumeSubplot = new XYPlot( volumeDataset, axis, volumeAxis, timeRenderer );

        volumeSubplot.setBackgroundPaint( Color.WHITE );
//        volumeSubplot.setDomainGridlinesVisible( false );
//        volumeSubplot.setRangeGridlinesVisible( false );

        /**
         * Create chart main plot with two subplots (candlestickSubplot,volumeSubplot)
         */
        // Create mainPlot
        final CombinedDomainXYPlot mainPlot = new CombinedDomainXYPlot();
        mainPlot.setOutlinePaint( null );
        mainPlot.setGap( 10.0 );
//        mainPlot.add( candlestickSubplot );
        mainPlot.add( candlestickSubplot, 3 );
        mainPlot.add( volumeSubplot, 2 );
        mainPlot.setOrientation( PlotOrientation.VERTICAL );

        JFreeChart chart = new JFreeChart( mainPlot );
        chart.removeLegend();
        return chart;
    }


    /**
     * Fill series with data.
     */
    public void addCandle( final AssetCandleMessageValue candle,
                           final Long index )
    {
        // Add bar to the data. Let's repeat the same bar
        final FixedMillisecond t = new FixedMillisecond( index );
        ohlcSeries.add( t, candle.getOpen(), candle.getHigh(), candle.getLow(), candle.getClose() );
        volumeSeries.add( t, candle.getVolume() );
    }
}
