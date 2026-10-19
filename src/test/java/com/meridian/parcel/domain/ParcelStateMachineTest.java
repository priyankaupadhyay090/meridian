package com.meridian.parcel.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParcelStateMachineTest {

    private final ParcelStateMachine machine = new ParcelStateMachine();

    @Test
    void newParcelAcceptsPickUp() {
        TransitionResult result = machine.apply(ParcelState.CREATED, ParcelEventType.PICKED_UP);

        assertThat(result.accepted()).isTrue();
        assertThat(result.to()).isEqualTo(ParcelState.PICKED_UP);
    }

    @Test
    void rejectsAnImpossibleSkipFromCreatedToDelivered() {
        TransitionResult result = machine.apply(ParcelState.CREATED, ParcelEventType.DELIVERED);

        assertThat(result.accepted()).isFalse();
        assertThat(result.to()).isEqualTo(ParcelState.CREATED);
        assertThat(result.reason()).contains("DELIVERED", "CREATED");
    }

    @Test
    void deliveredParcelAcceptsNothing() {
        for (ParcelEventType type : ParcelEventType.values()) {
            assertThat(machine.apply(ParcelState.DELIVERED, type).accepted()).isFalse();
        }
    }

    @Test
    void parcelCanRecoverFromAnException() {
        TransitionResult result = machine.apply(ParcelState.EXCEPTION, ParcelEventType.IN_TRANSIT);

        assertThat(result.accepted()).isTrue();
        assertThat(result.to()).isEqualTo(ParcelState.IN_TRANSIT);
    }
}
