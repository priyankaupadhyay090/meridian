package com.meridian.parcel.adapter.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.parcel.application.ParcelService;
import com.meridian.parcel.domain.ParcelEventType;
import com.meridian.routing.domain.CustomerTier;
import com.meridian.routing.domain.ServiceLevel;
import com.meridian.routing.domain.ShipmentProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(name = "meridian.kafka.enabled", havingValue = "true")
public class CourierSimulator {

    private static final List<ParcelEventType> JOURNEY = List.of(ParcelEventType.PICKED_UP,
            ParcelEventType.IN_TRANSIT, ParcelEventType.LAST_MILE, ParcelEventType.DELIVERED);
    private static final Duration PAUSE_BETWEEN_WAVES = Duration.ofSeconds(8);

    private enum Fault { DUPLICATE, OUT_OF_ORDER, BROKEN_JSON, UNKNOWN_PARCEL }

    private record Planned(String parcelId, Fault fault) {
    }

    private final KafkaTemplate<String, String> kafka;
    private final ParcelService parcels;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final String topic;
    private final ExecutorService runner = Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
    private final AtomicBoolean running = new AtomicBoolean();

    public CourierSimulator(KafkaTemplate<String, String> kafka, ParcelService parcels, ObjectMapper mapper,
                            Clock clock, @Value("${meridian.kafka.topic}") String topic) {
        this.kafka = kafka;
        this.parcels = parcels;
        this.mapper = mapper;
        this.clock = clock;
        this.topic = topic;
    }

    public SimulationReport start(SimulationRequest request, String user) {
        if (!running.compareAndSet(false, true)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "a simulation is already running");
        }
        String runId = "sim-" + UUID.randomUUID().toString().substring(0, 6);
        Random random = new Random();
        List<Planned> plan = new ArrayList<>();
        Map<String, Integer> faults = new LinkedHashMap<>();
        for (int i = 1; i <= request.parcels(); i++) {
            String parcelId = "%s-%04d".formatted(runId, i);
            parcels.register(parcelId, profile(random), user);
            Fault fault = random.nextDouble() < request.faultShare()
                    ? Fault.values()[random.nextInt(Fault.values().length)] : null;
            if (fault != null) {
                faults.merge(fault.name(), 1, Integer::sum);
            }
            plan.add(new Planned(parcelId, fault));
        }
        int messages = request.parcels() * JOURNEY.size()
                + faults.getOrDefault(Fault.DUPLICATE.name(), 0) + faults.getOrDefault(Fault.BROKEN_JSON.name(), 0)
                + faults.getOrDefault(Fault.UNKNOWN_PARCEL.name(), 0);
        runner.submit(() -> {
            try {
                send(plan);
            } finally {
                running.set(false);
            }
        });
        return new SimulationReport(runId, request.parcels(), messages, faults);
    }

    private void send(List<Planned> plan) {
        for (int wave = 0; wave < JOURNEY.size(); wave++) {
            for (Planned parcel : plan) {
                ParcelEventType type = JOURNEY.get(wave);
                if (parcel.fault() == Fault.OUT_OF_ORDER && wave >= 2) {
                    type = JOURNEY.get(wave == 2 ? 3 : 2);
                }
                String eventId = "%s-e%d".formatted(parcel.parcelId(), wave + 1);
                publish(parcel.parcelId(), event(eventId, parcel.parcelId(), type));
                if (wave == 1 && parcel.fault() == Fault.DUPLICATE) {
                    publish(parcel.parcelId(), event(eventId, parcel.parcelId(), type));
                }
                if (wave == 1 && parcel.fault() == Fault.BROKEN_JSON) {
                    publish(parcel.parcelId(), "{\"eventId\": \"" + eventId + "-x\", \"type\": IN_TRANSIT");
                }
                if (wave == 1 && parcel.fault() == Fault.UNKNOWN_PARCEL) {
                    String ghost = parcel.parcelId() + "-unknown";
                    publish(ghost, event(eventId + "-u", ghost, type));
                }
            }
            kafka.flush();
            if (wave < JOURNEY.size() - 1) {
                pause();
            }
        }
    }

    private void publish(String key, String value) {
        kafka.send(topic, key, value);
    }

    private String event(String eventId, String parcelId, ParcelEventType type) {
        try {
            return mapper.writeValueAsString(new ParcelEventMessage(eventId, parcelId, type, clock.instant()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ShipmentProfile profile(Random random) {
        String postcode = "%05d".formatted(10_000 + random.nextInt(89_999));
        ServiceLevel level = ServiceLevel.values()[random.nextInt(ServiceLevel.values().length)];
        CustomerTier tier = CustomerTier.values()[random.nextInt(CustomerTier.values().length)];
        return new ShipmentProfile(postcode, level, 200 + random.nextInt(30_000), tier);
    }

    private static void pause() {
        try {
            Thread.sleep(PAUSE_BETWEEN_WAVES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
