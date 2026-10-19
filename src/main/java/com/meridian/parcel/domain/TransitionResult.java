package com.meridian.parcel.domain;

public record TransitionResult(ParcelState from, ParcelState to, boolean accepted, String reason) {

    public static TransitionResult accepted(ParcelState from, ParcelState to) {
        return new TransitionResult(from, to, true, null);
    }

    public static TransitionResult rejected(ParcelState current, ParcelEventType eventType) {
        return new TransitionResult(current, current, false,
                "%s is not a valid transition from %s".formatted(eventType, current));
    }
}
