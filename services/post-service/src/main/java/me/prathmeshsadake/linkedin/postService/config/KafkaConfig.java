package me.prathmeshsadake.linkedin.postService.config;

import me.prathmeshsadake.linkedin.postService.events.PostCreatedEvent;
import me.prathmeshsadake.linkedin.postService.events.PostLikedEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    public static final String POST_CREATED_TOPIC = "post-created";
    public static final String POST_LIKED_TOPIC = "post-liked";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public org.apache.kafka.clients.admin.NewTopic postCreatedTopic() {
        return org.springframework.kafka.config.TopicBuilder.name(POST_CREATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public org.apache.kafka.clients.admin.NewTopic postLikedTopic() {
        return org.springframework.kafka.config.TopicBuilder.name(POST_LIKED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public ProducerFactory<String, PostCreatedEvent> postCreatedProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerProps());
    }

    @Bean
    public KafkaTemplate<String, PostCreatedEvent> postCreatedKafkaTemplate(
            ProducerFactory<String, PostCreatedEvent> postCreatedProducerFactory) {
        return new KafkaTemplate<>(postCreatedProducerFactory);
    }

    @Bean
    public ProducerFactory<String, PostLikedEvent> postLikedProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerProps());
    }

    @Bean
    public KafkaTemplate<String, PostLikedEvent> postLikedKafkaTemplate(
            ProducerFactory<String, PostLikedEvent> postLikedProducerFactory) {
        return new KafkaTemplate<>(postLikedProducerFactory);
    }

    private Map<String, Object> producerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return props;
    }
}
