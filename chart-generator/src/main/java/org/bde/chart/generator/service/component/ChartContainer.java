package org.bde.chart.generator.service.component;

import org.jfree.chart.ChartPanel;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.util.List;


public class ChartContainer
      extends JPanel
{
    public ChartContainer( final List<CandlestickChart> panels )
    {
        JFrame.setDefaultLookAndFeelDecorated( false );

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

        frame.setContentPane( panels.get( 0 ) );

        frame.pack();
        frame.setVisible( true );
    }
}
