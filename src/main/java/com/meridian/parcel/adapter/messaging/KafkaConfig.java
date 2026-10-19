package com.meridian.parcel.adapter.messaging;

import com.meridian.common.NotFoundException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
@ConditionalOnProperty(name = "meridian.kafka.enabled", havingValue = "true")
public class KafkaConfig {

    @Bean
    NewTopic parcelEvents(@Value("${meridian.kafka.topic}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic parcelEventsDeadLetters(@Value("${meridian.kafka.topic}") String topic) {
        return TopicBuilder.name(topic + ".DLT").partitions(3).replicas(1).build();
    }

    @Bean
    DefaultErrorHandler errorHandler(KafkaTemplate<String, String> template, ParcelEventMetrics metrics,
                                     @Value("${meridian.kafka.topic}") String topic) {
        DeadLetterPublishingRecoverer deadLetters = new DeadLetterPublishingRecoverer(template,
                (record, failure) -> new TopicPartition(topic + ".DLT", record.partition()));
        ExponentialBackOff backOff = new ExponentialBackOff(200, 2);
        backOff.setMaxAttempts(3);
        DefaultErrorHandler handler = new DefaultErrorHandler((record, failure) -> {
            deadLetters.accept(record, failure);
            metrics.count("DEAD_LETTER");
        }, backOff);
        handler.addNotRetryableExceptions(InvalidParcelEventException.class, NotFoundException.class);
        return handler;
    }
}
