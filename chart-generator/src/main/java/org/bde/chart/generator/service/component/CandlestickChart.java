package org.bde.chart.generator.service.component;

import lombok.Data;
import org.bde.chart.generator.model.Candle;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
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
import java.text.SimpleDateFormat;

@Data
public class CandlestickChart
      extends JPanel
{
    private OHLCSeries ohlcSeries;

    private TimeSeries volumeSeries;


    public CandlestickChart()
    {
        final JFreeChart candlestickChart = createChart();
        final ChartPanel chartPanel = new ChartPanel( candlestickChart );
        chartPanel.setPreferredSize( new Dimension( 500, 500 ) );
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
        final NumberAxis priceAxis = new NumberAxis( "Price" );
        priceAxis.setAutoRangeIncludesZero( false );
        final CandlestickRenderer candlestickRenderer = new CandlestickRenderer( CandlestickRenderer.WIDTHMETHOD_AVERAGE );

        // Create candlestickSubplot
        final XYPlot candlestickSubplot = new XYPlot( candlestickDataset, null, priceAxis, candlestickRenderer );
        candlestickSubplot.setBackgroundPaint( Color.WHITE );

        /**
         * Creating volume subplot
         */
        // creates TimeSeriesCollection as a volume dataset for volume chart
        final TimeSeriesCollection volumeDataset = new TimeSeriesCollection();
        volumeSeries = new TimeSeries( "Volume" );
        volumeDataset.addSeries( volumeSeries );
        // Create volume chart volumeAxis
        final NumberAxis volumeAxis = new NumberAxis( "Volume" );
        volumeAxis.setAutoRangeIncludesZero( false );
        // Set to no decimal
        volumeAxis.setNumberFormatOverride( new DecimalFormat( "0" ) );

        // Create volume chart renderer
        XYBarRenderer timeRenderer = new XYBarRenderer();
        timeRenderer.setShadowVisible( false );
        timeRenderer.setBaseToolTipGenerator( new StandardXYToolTipGenerator( "Volume--> Time={1} Size={2}",
                                                                              new SimpleDateFormat( "kk:mm" ), new DecimalFormat( "0" ) ) );
        // Create volumeSubplot
        final XYPlot volumeSubplot = new XYPlot( volumeDataset, null, volumeAxis, timeRenderer );
        volumeSubplot.setBackgroundPaint( Color.WHITE );

        /**
         * Create chart main plot with two subplots (candlestickSubplot,volumeSubplot)
         */
        // Create mainPlot
        final CombinedDomainXYPlot mainPlot = new CombinedDomainXYPlot();
        mainPlot.setGap( 10.0 );
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
    public void addCandle( final Candle candle )
    {
        // Add bar to the data. Let's repeat the same bar
        final FixedMillisecond t = new FixedMillisecond( candle.getTimestamp() );
        ohlcSeries.add( t, candle.getOpen(), candle.getHigh(), candle.getLow(), candle.getClose() );
        volumeSeries.add( t, candle.getVolume() );
    }
}
