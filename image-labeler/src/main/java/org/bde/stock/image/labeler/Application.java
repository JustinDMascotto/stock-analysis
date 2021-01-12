package org.bde.stock.image.labeler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;


@Slf4j
@EnableConfigurationProperties
@SpringBootApplication
@PropertySource( "classpath:shared.properties" )
public class Application
{
    public static void main( String[] args )
    {
        final var appBuilder = new SpringApplicationBuilder( Application.class );
        appBuilder.headless( false ).run( args );
    }
}

