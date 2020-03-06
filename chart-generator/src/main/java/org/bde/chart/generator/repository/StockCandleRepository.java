package org.bde.chart.generator.repository;

import org.bde.chart.generator.entity.StockCandleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;


@Repository
public interface StockCandleRepository
      extends JpaRepository<StockCandleEntity, Long>
{

    @Query( "SELECT sc " +
            "FROM stock_candle sc " +
            "WHERE DATE(sc.timestamp) = DATE(:date) " +
            "AND sc.ticker = :ticker " +
            "AND sc.interval = :interval " +
            "ORDER BY sc.timestamp DESC" )
    List<StockCandleEntity> findByTimestampAndTicker( @Param( "date" ) final LocalDate date,
                                                      @Param( "ticker" ) final String ticker,
                                                      @Param( "interval" ) final Integer interval );
}
