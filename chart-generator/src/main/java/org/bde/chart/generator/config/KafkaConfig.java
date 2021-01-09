package org.bde.chart.generator.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.streams.kstream.TimeWindowedDeserializer;
import org.apache.kafka.streams.kstream.Windowed;
import org.bde.stock.common.kafka.serde.AssetCandleAggregator;
import org.bde.stock.common.message.AssetCandleMessageValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Configuration
@EnableKafka
public class KafkaConfig
{
    protected Map<String, Object> consumerConfigs( final Environment environment )
    {
        final Map<String, Object> props = new HashMap<>();

        props.put( org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, TimeWindowedDeserializer.class );
        props.put( org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class );

        props.put( org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, environment.getProperty( "spring.kafka.bootstrap-servers" ) );
        props.put( org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, environment.getProperty( "spring.application.name" ) );

        return props;
    }

    protected ConsumerFactory<Windowed<String>, AssetCandleAggregator> consumerFactory( final Environment environment )
    {
        final StringDeserializer deserializer = new StringDeserializer();
        deserializer.configure( Collections.emptyMap(), true );
        final TimeWindowedDeserializer keyDeserializer = new TimeWindowedDeserializer<>( deserializer );
        return new DefaultKafkaConsumerFactory<>( consumerConfigs( environment ),
                                                  keyDeserializer,
                                                  new JsonDeserializer<>( new TypeReference<List<AssetCandleMessageValue>>() {}, new ObjectMapper().registerModule( new JavaTimeModule() ) ) );
    }

    @Bean( "windowedAssetCandleListener" )
    protected ConcurrentKafkaListenerContainerFactory<Windowed<String>, AssetCandleAggregator> kafkaListenerContainerFactory( final Environment environment )
    {
        final ConcurrentKafkaListenerContainerFactory<Windowed<String>, AssetCandleAggregator> factory =
              new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory( consumerFactory( environment ) );
        return factory;
    }
}

