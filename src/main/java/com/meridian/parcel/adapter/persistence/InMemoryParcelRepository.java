package com.meridian.parcel.adapter.persistence;

import com.meridian.parcel.application.ParcelRepository;
import com.meridian.parcel.domain.Parcel;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryParcelRepository implements ParcelRepository {

    private final Map<String, Parcel> parcels = new ConcurrentHashMap<>();
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    @Override
    public void save(Parcel parcel) {
        parcels.put(parcel.id(), parcel);
    }

    @Override
    public Optional<Parcel> findById(String id) {
        return Optional.ofNullable(parcels.get(id));
    }

    @Override
    public List<Parcel> findAll() {
        return parcels.values().stream().sorted(Comparator.comparing(Parcel::id)).toList();
    }

    @Override
    public boolean markProcessed(String eventId) {
        return processedEventIds.add(eventId);
    }
}
