package org.bde.chart.generator.service;

import org.bde.chart.generator.model.Candle;
import org.bde.chart.generator.service.component.CandlestickChart;
import org.bde.chart.generator.service.component.ImageContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;


@Service
public class ImageGeneratorService
{
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss" );

    private static final String BUY_FOLDER = "Buy";

    private static final String SELL_FOLDER = "Sell";

    private static final String DO_NOTHING_FOLDER = "Nothing";

    @Value( "${bde.stock-analysis.image-generator.output-dir}" )
    private String outputDir;


    public void generateGraphs( final String ticker,
                                final List<Integer> outputIntervals )
          throws IOException, InterruptedException
    {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try ( final InputStream is = classLoader.getResourceAsStream( "intraday_15min_AAPL.csv" );
              final BufferedReader reader = new BufferedReader( new InputStreamReader( is ) ) )
        {
            // read header line
            String line = reader.readLine();
            final CandlestickChart candlestickChart1 = new CandlestickChart();
            final CandlestickChart candlestickChart2 = new CandlestickChart();
            var count = 0;
            while ( ( line = reader.readLine() ) != null && count < 20 )
            {
                final Candle candle = toCandle( line );
                if ( count < 10 )
                {
                    candlestickChart1.addCandle( candle );
                    count++;
                }
                else
                {
                    candlestickChart2.addCandle( candle );
                    count++;
                }
            }

            final ImageContainer container = new ImageContainer( Arrays.asList( candlestickChart1, candlestickChart2 ) );
            toImage( container.getContentPane(), "save.png" );

            Thread.sleep( 5000 );

            System.out.println( container );
        }
    }


    private Candle toCandle( final String line )
    {
        final String[] parts = line.split( "," );
        final LocalDateTime timestamp = LocalDateTime.parse( parts[0], FORMATTER );
        final LocalDateTime startOfDay = timestamp.toLocalDate().atStartOfDay();
        final Long time = ( Duration.between( startOfDay, timestamp ).toMinutes() / 15 ) - 59;

        return Candle.builder()
                     .timestamp( time )
                     .open( Double.parseDouble( parts[1] ) )
                     .high( Double.parseDouble( parts[2] ) )
                     .low( Double.parseDouble( parts[3] ) )
                     .close( Double.parseDouble( parts[4] ) )
                     .volume( Integer.parseInt( parts[5] ) )
                     .build();
    }


    private void toImage( final Container imageContentPane,
                          final String savePath )
    {
        final Dimension size = imageContentPane.getSize();
        final BufferedImage image = new BufferedImage( size.width, size.height, BufferedImage.TYPE_INT_RGB );
        final Graphics2D g2 = image.createGraphics();
        imageContentPane.paint( g2 );
        try
        {
            ImageIO.write( image, "png", new File( savePath ) );
            System.out.println( "Panel saved as Image." );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }
}
