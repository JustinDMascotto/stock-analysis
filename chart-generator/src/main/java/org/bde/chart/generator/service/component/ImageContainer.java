package org.bde.chart.generator.service.component;


import lombok.Getter;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Container;
import java.awt.GridLayout;
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
}
