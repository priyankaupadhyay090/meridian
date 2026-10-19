package com.meridian.change.adapter.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.change.application.SampleShipmentSource;
import com.meridian.change.domain.SampleShipment;
import com.meridian.common.SeedResources;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SeedShipmentSource implements SampleShipmentSource {

    private final List<SampleShipment> shipments;

    public SeedShipmentSource(ObjectMapper mapper) {
        this.shipments = List.copyOf(SeedResources.read(mapper, "seed/shipments.json", new TypeReference<>() {
        }));
    }

    @Override
    public List<SampleShipment> recordedShipments() {
        return shipments;
    }
}
