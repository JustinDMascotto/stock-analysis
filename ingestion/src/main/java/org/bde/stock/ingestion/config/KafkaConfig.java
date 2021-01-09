package org.bde.stock.ingestion.config;


import org.apache.kafka.streams.StreamsConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;

import java.util.HashMap;
import java.util.Random;


@Configuration
@EnableKafkaStreams
public class KafkaConfig
{
    @Bean( name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME )
    public KafkaStreamsConfiguration kafkaStreamsConfiguration( final Environment environment )
    {
        final var props = new HashMap<String,Object>();
        props.put( StreamsConfig.APPLICATION_ID_CONFIG, "ingestion" + new Random().nextLong() );
        props.put( StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, environment.getProperty( "spring.kafka.bootstrap-servers" ) );

        return new KafkaStreamsConfiguration( props );
    }
}
