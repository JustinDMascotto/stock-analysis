package org.bde.chart.generator.config;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class DataSourceConfig
{
    @Bean
    @ConfigurationProperties( prefix = "bde.stock-analysis.datasource" )
    public DataSource dataSource()
    {
        return DataSourceBuilder.create().type( DataSource.class ).build();
    }
}
