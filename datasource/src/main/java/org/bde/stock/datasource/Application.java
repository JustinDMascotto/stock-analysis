package org.bde.stock.datasource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySources;
import org.springframework.scheduling.annotation.EnableScheduling;


@Slf4j
@EnableConfigurationProperties
@SpringBootApplication
@EnableScheduling
public class Application
{
    public static void main( String[] args )
    {
        SpringApplication.run( Application.class, args );
    }
}

