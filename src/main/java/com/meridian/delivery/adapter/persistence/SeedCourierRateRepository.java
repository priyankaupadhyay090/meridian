package com.meridian.delivery.adapter.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.common.SeedResources;
import com.meridian.delivery.application.CourierRateRepository;
import com.meridian.delivery.domain.CourierRate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SeedCourierRateRepository implements CourierRateRepository {

    private final List<CourierRate> rates;

    public SeedCourierRateRepository(ObjectMapper mapper) {
        this.rates = List.copyOf(SeedResources.read(mapper, "seed/courier-rates.json", new TypeReference<>() {
        }));
    }

    @Override
    public List<CourierRate> rates() {
        return rates;
    }
}
