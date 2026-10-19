package com.meridian.parcel.domain;

import java.time.Instant;

public record ParcelEvent(String eventId, String parcelId, ParcelEventType type, Instant occurredAt) {
}
