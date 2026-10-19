package com.meridian.parcel.application;

import com.meridian.parcel.domain.Parcel;

import java.util.List;
import java.util.Optional;

public interface ParcelRepository {

    void save(Parcel parcel);

    Optional<Parcel> findById(String id);

    List<Parcel> findAll();

    boolean markProcessed(String eventId);
}
