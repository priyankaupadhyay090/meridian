package com.meridian.parcel.domain;

import java.time.Instant;

public record ReviewItem(String id, String parcelId, String eventId, ParcelState currentState,
                         ParcelEventType rejectedEvent, String reason, Instant createdAt) {
}
