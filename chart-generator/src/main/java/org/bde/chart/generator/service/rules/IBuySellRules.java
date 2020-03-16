package org.bde.chart.generator.service.rules;

import org.bde.chart.generator.entity.StockCandleEntity;

import javax.imageio.ImageIO;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;


public interface IBuySellRules
{
    void createChartImages( String ticker,
                            LocalDate candlesDate,
                            LinkedHashMap<LocalDateTime, StockCandleEntity> originalCandles,
                            LinkedHashMap<LocalDateTime, StockCandleEntity> composedCandles );

    Integer getMaxCandlesInWindowLargesTimeFrame();

    void createImageAndDetermineBuyOrSell( Integer numberOfCandlesInWindowHighestTimeFrame,
                                           Integer slidingWindowIndex,
                                           LinkedHashMap<LocalDateTime, StockCandleEntity> fiveMinuteCandles,
                                           LinkedHashMap<LocalDateTime, StockCandleEntity> fifteenMinuteCandles );

    default void toImage( final Container imageContentPane,
                          final String savePath )
    {
        final Dimension size = imageContentPane.getSize();
        final BufferedImage image = new BufferedImage( size.width, size.height - 20, BufferedImage.TYPE_INT_RGB );
        final Graphics2D g2 = image.createGraphics();
        imageContentPane.paint( g2 );
        try
        {
            ImageIO.write( image, "png", new File( savePath ) );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }
}
