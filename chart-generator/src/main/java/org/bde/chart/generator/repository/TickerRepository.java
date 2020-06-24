package org.bde.chart.generator.repository;

import org.bde.chart.generator.entity.TickerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface TickerRepository
      extends JpaRepository<TickerEntity,Long>
{
    TickerEntity findByName( String name );
}
