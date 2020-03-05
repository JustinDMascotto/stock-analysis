package org.bde.chart.generator.service;

import org.bde.chart.generator.model.Candle;
import org.bde.chart.generator.service.component.CandlestickChart;
import org.bde.chart.generator.service.component.ChartContainer;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;


@Service
public class ChartGeneratorService
{
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss" );


    public void generateGraph()
          throws IOException, InterruptedException
    {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try ( final InputStream is = classLoader.getResourceAsStream( "intraday_15min_AAPL.csv" );
              final BufferedReader reader = new BufferedReader( new InputStreamReader( is ) ) )
        {
            // read header line
            String line = reader.readLine();
            final CandlestickChart candlestickChart = new CandlestickChart();
            var count = 0;
            while ( ( line = reader.readLine() ) != null && count < 20 )
            {
                final Candle candle = toCandle( line );
                candlestickChart.addCandle( candle );
                count++;
            }

            final ChartContainer container = new ChartContainer( Collections.singletonList( candlestickChart ) );

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
}
