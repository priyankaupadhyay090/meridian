package com.meridian.parcel.adapter.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.meridian.audit.adapter.persistence.InMemoryAuditLog;
import com.meridian.common.NotFoundException;
import com.meridian.parcel.adapter.persistence.InMemoryParcelRepository;
import com.meridian.parcel.adapter.persistence.InMemoryReviewQueue;
import com.meridian.parcel.application.ParcelService;
import com.meridian.parcel.domain.ParcelState;
import com.meridian.routing.adapter.persistence.InMemoryRuleSetStore;
import com.meridian.routing.application.RoutingService;
import com.meridian.routing.domain.CarrierService;
import com.meridian.routing.domain.CustomerTier;
import com.meridian.routing.domain.RoutingRule;
import com.meridian.routing.domain.RuleMatch;
import com.meridian.routing.domain.ServiceLevel;
import com.meridian.routing.domain.ShipmentProfile;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParcelEventListenerTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-12T09:00:00Z"), ZoneOffset.UTC);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final ParcelService parcels = new ParcelService(new InMemoryParcelRepository(), new InMemoryReviewQueue(),
            new RoutingService(new InMemoryRuleSetStore(List.of(new RoutingRule("r", new RuleMatch("1", null, null,
                    null), 500, CarrierService.DHL_PAKET)))), new InMemoryAuditLog(clock), clock);
    private final ParcelEventListener listener = new ParcelEventListener(parcels,
            new ObjectMapper().registerModule(new JavaTimeModule()), new ParcelEventMetrics(registry, parcels));

    private static ConsumerRecord<String, String> record(String value) {
        return new ConsumerRecord<>("parcel-events", 0, 0, "P-1", value);
    }

    private static String event(String id, String type) {
        return "{\"eventId\":\"%s\",\"parcelId\":\"P-1\",\"type\":\"%s\",\"occurredAt\":\"2026-10-12T09:00:00Z\"}"
                .formatted(id, type);
    }

    private double count(String outcome) {
        return registry.counter("meridian.parcel.events", "outcome", outcome).count();
    }

    @Test
    void eventsMoveTheParcelAndADuplicateIsCountedButIgnored() {
        parcels.register("P-1", new ShipmentProfile("10115", ServiceLevel.STANDARD, 900, CustomerTier.BASIC), "test");

        listener.onEvent(record(event("e1", "PICKED_UP")));
        listener.onEvent(record(event("e1", "PICKED_UP")));
        listener.onEvent(record(event("e2", "IN_TRANSIT")));

        assertThat(parcels.find("P-1").state()).isEqualTo(ParcelState.IN_TRANSIT);
        assertThat(count("APPLIED")).isEqualTo(2);
        assertThat(count("DUPLICATE_IGNORED")).isEqualTo(1);
    }

    @Test
    void brokenOrIncompleteEventsAndUnknownParcelsFailSoTheErrorHandlerCanDeadLetterThem() {
        assertThatThrownBy(() -> listener.onEvent(record("{\"eventId\": \"x\", \"type\": IN_TRANSIT")))
                .isInstanceOf(InvalidParcelEventException.class);
        assertThatThrownBy(() -> listener.onEvent(record("{\"eventId\":\"x\",\"parcelId\":\"P-1\"}")))
                .isInstanceOf(InvalidParcelEventException.class);
        assertThatThrownBy(() -> listener.onEvent(record(event("e9", "PICKED_UP"))))
                .isInstanceOf(NotFoundException.class);
    }
}
