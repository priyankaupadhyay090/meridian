package com.meridian.parcel.adapter.messaging;

import com.meridian.parcel.domain.ParcelEvent;
import com.meridian.parcel.domain.ParcelEventType;

import java.time.Instant;

public record ParcelEventMessage(String eventId, String parcelId, ParcelEventType type, Instant occurredAt) {

    ParcelEvent toEvent() {
        if (eventId == null || eventId.isBlank() || parcelId == null || parcelId.isBlank() || type == null
                || occurredAt == null) {
            throw new InvalidParcelEventException("event is missing eventId, parcelId, type or occurredAt");
        }
        return new ParcelEvent(eventId, parcelId, type, occurredAt);
    }
}
