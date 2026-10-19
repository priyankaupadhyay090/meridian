package com.meridian.parcel.adapter.messaging;

import com.meridian.parcel.application.ParcelService;
import com.meridian.parcel.domain.ParcelState;
import com.meridian.routing.domain.CustomerTier;
import com.meridian.routing.domain.ServiceLevel;
import com.meridian.routing.domain.ShipmentProfile;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
        "meridian.kafka.enabled=true",
        "spring.kafka.listener.auto-startup=true",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"})
@EmbeddedKafka(partitions = 3, topics = {"parcel-events", "parcel-events.DLT"})
@DirtiesContext
class ParcelEventsOnKafkaTest {

    @Autowired
    private KafkaTemplate<String, String> kafka;

    @Autowired
    private ParcelService parcels;

    @Autowired
    private EmbeddedKafkaBroker broker;

    private static String event(String id, String parcelId, String type) {
        return "{\"eventId\":\"%s\",\"parcelId\":\"%s\",\"type\":\"%s\",\"occurredAt\":\"2026-10-12T09:00:00Z\"}"
                .formatted(id, parcelId, type);
    }

    @Test
    void aBrokenEventGoesToTheDeadLetterTopicOnItsOwnPartitionAndTheEventsBehindItStillArrive() throws Exception {
        parcels.register("P-42", new ShipmentProfile("10115", ServiceLevel.STANDARD, 900, CustomerTier.BASIC), "test");

        kafka.send("parcel-events", "P-42", event("e1", "P-42", "PICKED_UP")).get();
        RecordMetadata broken = kafka.send("parcel-events", "P-42", "{\"eventId\": \"bad\", \"type\": IN_TRANSIT").get()
                .getRecordMetadata();
        kafka.send("parcel-events", "P-42", event("e2", "P-42", "IN_TRANSIT")).get();

        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(parcels.find("P-42").state()).isEqualTo(ParcelState.IN_TRANSIT));

        Map<String, Object> config = KafkaTestUtils.consumerProps("dlt-check", "false", broker);
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(),
                new StringDeserializer()).createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, "parcel-events.DLT");
            ConsumerRecord<String, String> deadLetter = KafkaTestUtils.getSingleRecord(consumer, "parcel-events.DLT",
                    Duration.ofSeconds(20));
            assertThat(deadLetter.key()).isEqualTo("P-42");
            assertThat(deadLetter.partition()).isEqualTo(broken.partition());
            assertThat(new String(deadLetter.headers().lastHeader("kafka_dlt-exception-message").value()))
                    .contains("not valid JSON");
        }
    }
}
