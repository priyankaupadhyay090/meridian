package com.meridian.parcel.adapter.web;

import com.meridian.parcel.domain.ParcelEvent;
import com.meridian.parcel.domain.ParcelEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ParcelEventRequest(@NotBlank String eventId, @NotBlank String parcelId, @NotNull ParcelEventType type,
                                 Instant occurredAt) {

    ParcelEvent toEvent(Instant receivedAt) {
        return new ParcelEvent(eventId, parcelId, type, occurredAt == null ? receivedAt : occurredAt);
    }
}
