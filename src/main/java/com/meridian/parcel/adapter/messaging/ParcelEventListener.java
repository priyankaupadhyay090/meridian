package com.meridian.parcel.adapter.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.parcel.application.ParcelService;
import com.meridian.parcel.domain.EventResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "meridian.kafka.enabled", havingValue = "true")
public class ParcelEventListener {

    private final ParcelService parcels;
    private final ObjectMapper mapper;
    private final ParcelEventMetrics metrics;

    public ParcelEventListener(ParcelService parcels, ObjectMapper mapper, ParcelEventMetrics metrics) {
        this.parcels = parcels;
        this.mapper = mapper;
        this.metrics = metrics;
    }

    @KafkaListener(topics = "${meridian.kafka.topic}", concurrency = "3")
    public void onEvent(ConsumerRecord<String, String> record) {
        ParcelEventMessage message = parse(record.value());
        EventResult result = metrics.processing().record(() -> parcels.apply(message.toEvent()));
        metrics.count(result.outcome().name());
    }

    private ParcelEventMessage parse(String value) {
        try {
            return mapper.readValue(value, ParcelEventMessage.class);
        } catch (JsonProcessingException e) {
            throw new InvalidParcelEventException("event is not valid JSON for a parcel event", e);
        }
    }
}
