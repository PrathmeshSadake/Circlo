package me.prathmeshsadake.linkedin.notificationService.config;

import me.prathmeshsadake.linkedin.notificationService.events.ConnectionRequestedEvent;
import me.prathmeshsadake.linkedin.notificationService.events.PostCreatedEvent;
import me.prathmeshsadake.linkedin.notificationService.events.PostLikedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PostCreatedEvent> postCreatedFactory() {
        return factory(PostCreatedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PostLikedEvent> postLikedFactory() {
        return factory(PostLikedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ConnectionRequestedEvent> connectionRequestedFactory() {
        return factory(ConnectionRequestedEvent.class);
    }

    private <T> ConcurrentKafkaListenerContainerFactory<String, T> factory(Class<T> type) {
        JsonDeserializer<T> deserializer = new JsonDeserializer<>(type);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer));
        factory.setConcurrency(3);
        return factory;
    }
}
