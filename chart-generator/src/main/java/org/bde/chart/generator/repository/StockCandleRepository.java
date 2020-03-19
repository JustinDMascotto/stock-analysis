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
            "AND sc.ticker.name = :tickerName " +
            "AND sc.interval = :interval " +
            "ORDER BY sc.timestamp DESC" )
    List<StockCandleEntity> findByTimestampAndTicker( @Param( "date" ) final LocalDate date,
                                                      @Param( "tickerName" ) final String tickerName,
                                                      @Param( "interval" ) final Integer interval );


    @Query( "SELECT sc " +
            "FROM stock_candle sc " +
            "WHERE sc.timestamp = ( SELECT MIN( sc1.timestamp ) " +
            "                       FROM stock_candle sc1 " +
            "                       WHERE sc1.interval = :interval " +
            "                       AND sc1.ticker.name = :tickerName ) " +
            "AND sc.ticker.name = :tickerName" )
    StockCandleEntity findEarliestByTickerAndInterval( @Param( "tickerName" ) final String tickerName,
                                                       @Param( "interval" ) final Integer interval );
}
