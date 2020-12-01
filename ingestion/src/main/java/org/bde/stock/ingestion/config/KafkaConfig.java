package org.bde.stock.ingestion.config;


import org.apache.kafka.streams.StreamsConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;

import java.util.HashMap;


@Configuration
@EnableKafkaStreams
public class KafkaConfig
{
    @Bean( name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME )
    public KafkaStreamsConfiguration kafkaStreamsConfiguration()
    {
        final var props = new HashMap<String,Object>();
        props.put( StreamsConfig.APPLICATION_ID_CONFIG, "kafka-stream" );
        props.put( StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092" );

        return new KafkaStreamsConfiguration( props );
    }
}
