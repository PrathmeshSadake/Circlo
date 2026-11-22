package me.prathmeshsadake.linkedin.connectionService.config;

import me.prathmeshsadake.linkedin.connectionService.events.ConnectionRequestedEvent;
import me.prathmeshsadake.linkedin.connectionService.events.UserCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    public static final String USER_CREATED_TOPIC = "user-created";
    public static final String CONNECTION_REQUESTED_TOPIC = "connection-requested";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public org.apache.kafka.clients.admin.NewTopic connectionRequestedTopic() {
        return org.springframework.kafka.config.TopicBuilder.name(CONNECTION_REQUESTED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public KafkaTemplate<String, ConnectionRequestedEvent> connectionRequestedKafkaTemplate() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserCreatedEvent> userCreatedFactory() {
        return listenerFactory(UserCreatedEvent.class, "connection-service");
    }

    private <T> ConcurrentKafkaListenerContainerFactory<String, T> listenerFactory(Class<T> type, String groupId) {
        JsonDeserializer<T> deserializer = new JsonDeserializer<>(type);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        DefaultKafkaConsumerFactory<String, T> consumerFactory =
                new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
        ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);
        return factory;
    }
}
