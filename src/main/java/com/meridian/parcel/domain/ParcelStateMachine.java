package com.meridian.parcel.domain;

public class ParcelStateMachine {

    public TransitionResult apply(ParcelState current, ParcelEventType eventType) {
        if (!current.accepts(eventType)) {
            return TransitionResult.rejected(current, eventType);
        }
        return TransitionResult.accepted(current, ParcelState.after(eventType));
    }
}
