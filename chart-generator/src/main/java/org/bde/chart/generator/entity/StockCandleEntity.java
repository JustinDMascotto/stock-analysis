package org.bde.chart.generator.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity( name = "stock_candle" )
public class StockCandleEntity
{
    @Id
    @SequenceGenerator( name = "stock_candle_seq",
                        sequenceName = "stock_candle_seq",
                        allocationSize = 1 )
    @GeneratedValue( generator = "stock_candle_seq",
                     strategy = GenerationType.SEQUENCE )
    private Long id;

    private String ticker;

    private Double open;

    private Double close;

    private Double high;

    private Double low;

    private Integer volume;

    private Integer interval;

    @Convert( converter = Jsr310JpaConverters.LocalDateTimeConverter.class )
    private LocalDateTime timestamp;
}
