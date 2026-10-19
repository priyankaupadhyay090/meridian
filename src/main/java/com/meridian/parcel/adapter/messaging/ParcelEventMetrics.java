package com.meridian.parcel.adapter.messaging;

import com.meridian.parcel.application.ParcelService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class ParcelEventMetrics {

    private final MeterRegistry registry;
    private final Timer processing;

    public ParcelEventMetrics(MeterRegistry registry, ParcelService parcels) {
        this.registry = registry;
        this.processing = Timer.builder("meridian.parcel.event.processing")
                .description("Time to apply one parcel event")
                .publishPercentileHistogram()
                .register(registry);
        Gauge.builder("meridian.review.queue.size", parcels, service -> service.reviewQueue().size())
                .description("Parcel events waiting for a person")
                .register(registry);
    }

    public void count(String outcome) {
        Counter.builder("meridian.parcel.events")
                .description("Parcel events by outcome")
                .tag("outcome", outcome)
                .register(registry)
                .increment();
    }

    public Timer processing() {
        return processing;
    }
}
