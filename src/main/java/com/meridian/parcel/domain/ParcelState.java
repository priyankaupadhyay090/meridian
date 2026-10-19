package com.meridian.parcel.domain;

import java.util.EnumSet;
import java.util.Set;

public enum ParcelState {
    CREATED(0),
    PICKED_UP(1),
    CUSTOMS_HELD(2),
    CUSTOMS_CLEARED(3),
    IN_TRANSIT(4),
    LAST_MILE(5),
    DELIVERED(6),
    EXCEPTION(-1);

    private final int stage;

    ParcelState(int stage) {
        this.stage = stage;
    }

    public int stage() {
        return stage;
    }

    public boolean accepts(ParcelEventType eventType) {
        return allowedEvents().contains(eventType);
    }

    private Set<ParcelEventType> allowedEvents() {
        return switch (this) {
            case CREATED -> EnumSet.of(ParcelEventType.PICKED_UP, ParcelEventType.EXCEPTION);
            case PICKED_UP -> EnumSet.of(ParcelEventType.CUSTOMS_HELD, ParcelEventType.CUSTOMS_CLEARED,
                    ParcelEventType.IN_TRANSIT, ParcelEventType.EXCEPTION);
            case CUSTOMS_HELD -> EnumSet.of(ParcelEventType.CUSTOMS_CLEARED, ParcelEventType.EXCEPTION);
            case CUSTOMS_CLEARED -> EnumSet.of(ParcelEventType.IN_TRANSIT, ParcelEventType.EXCEPTION);
            case IN_TRANSIT -> EnumSet.of(ParcelEventType.CUSTOMS_HELD, ParcelEventType.LAST_MILE,
                    ParcelEventType.EXCEPTION);
            case LAST_MILE -> EnumSet.of(ParcelEventType.DELIVERED, ParcelEventType.EXCEPTION);
            case DELIVERED -> EnumSet.noneOf(ParcelEventType.class);
            case EXCEPTION -> EnumSet.of(ParcelEventType.PICKED_UP, ParcelEventType.CUSTOMS_CLEARED,
                    ParcelEventType.IN_TRANSIT);
        };
    }

    public static ParcelState after(ParcelEventType eventType) {
        return switch (eventType) {
            case PICKED_UP -> PICKED_UP;
            case CUSTOMS_HELD -> CUSTOMS_HELD;
            case CUSTOMS_CLEARED -> CUSTOMS_CLEARED;
            case IN_TRANSIT -> IN_TRANSIT;
            case LAST_MILE -> LAST_MILE;
            case DELIVERED -> DELIVERED;
            case EXCEPTION -> EXCEPTION;
        };
    }
}
