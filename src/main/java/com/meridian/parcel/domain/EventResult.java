package com.meridian.parcel.domain;

public record EventResult(String parcelId, String eventId, EventOutcome outcome, ParcelState state,
                          String reviewItemId) {
}
