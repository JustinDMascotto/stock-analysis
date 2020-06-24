package org.bde.chart.generator.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import java.time.LocalDateTime;
import java.util.Set;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString( exclude = "candles" )
@Entity( name = "ticker" )
public class TickerEntity
{
    @Id
    @SequenceGenerator( name = "ticker_seq",
                        sequenceName = "ticker_seq",
                        allocationSize = 1 )
    @GeneratedValue( generator = "ticker_seq",
                     strategy = GenerationType.SEQUENCE )
    private Long id;

    private String name;

    @Enumerated( EnumType.STRING )
    private MarketOpen marketOpen;

    @Convert( converter = Jsr310JpaConverters.LocalDateTimeConverter.class )
    private LocalDateTime marketOpenTime;

    @Convert( converter = Jsr310JpaConverters.LocalDateTimeConverter.class )
    private LocalDateTime marketCloseTime;

    @OneToMany( fetch = FetchType.LAZY )
    @JoinColumn( name = "ticker_id" )
    private Set<StockCandleEntity> candles;
}
