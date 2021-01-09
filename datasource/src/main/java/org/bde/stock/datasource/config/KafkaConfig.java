package org.bde.stock.datasource.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.bde.stock.common.message.AssetCandleMessageKey;
import org.bde.stock.common.message.AssetCandleMessageValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;


@Configuration
public class KafkaConfig
{
    private final Environment environment;


    @Autowired
    public KafkaConfig( final Environment environment )
    {
        this.environment = environment;
    }

    @Bean
    public ProducerFactory<AssetCandleMessageKey, AssetCandleMessageValue> producerFactory()
    {
        final Map<String, Object> properties = new HashMap<>();
        properties.put( ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, environment.getProperty( "spring.kafka.bootstrap-servers" ) );
        properties.put( ProducerConfig.MAX_BLOCK_MS_CONFIG, environment.getProperty( "spring.kafka.properties.max-block-ms" ) );
        properties.put( ProducerConfig.CLIENT_ID_CONFIG, environment.getProperty( "spring.kafka.client-id" ) );
        properties.put( ProducerConfig.LINGER_MS_CONFIG, environment.getProperty( "spring.kafka.producer.linger.ms" ) );
        properties.put( ProducerConfig.ACKS_CONFIG, environment.getProperty( "spring.kafka.producer.acks" ) );
        properties.put( ProducerConfig.RETRIES_CONFIG, environment.getProperty( "spring.kafka.producer.retries" ) );
        properties.put( ProducerConfig.RETRY_BACKOFF_MS_CONFIG, environment.getProperty( "spring.kafka.producer.retry.backoff.ms" ) );

        return new DefaultKafkaProducerFactory<>( properties,
                                                  new JsonSerializer<>(),
                                                  new JsonSerializer<>() );
    }


    @Bean
    public KafkaTemplate<AssetCandleMessageKey, AssetCandleMessageValue> kafkaTemplate()
    {
        return new KafkaTemplate<>( producerFactory() );
    }
}
