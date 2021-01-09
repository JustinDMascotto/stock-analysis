package org.bde.chart.generator.render;


import lombok.Getter;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;


public class ImageContainer
      extends JPanel
{
    @Getter
    private Container contentPane;


    public ImageContainer( final List<CandlestickChart> panels )
    {
        JFrame.setDefaultLookAndFeelDecorated( false );

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.setLayout( new GridLayout( 1, panels.size() ) );

        panels.forEach( frame::add );

        contentPane = frame.getContentPane();

        frame.pack();
        frame.setVisible( true );
    }


    public static void toImage( final Container imageContentPane,
                                final String savePath )
    {
        final Dimension size = imageContentPane.getSize();
        final BufferedImage image = new BufferedImage( size.width, size.height, BufferedImage.TYPE_INT_RGB );
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
