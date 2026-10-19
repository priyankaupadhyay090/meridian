package com.meridian.parcel.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RemediationGateTest {

    private final RemediationGate gate = new RemediationGate();

    @Test
    void lateEventForAnEarlierStageIsSafeToIgnore() {
        assertThat(gate.decide(ParcelState.LAST_MILE, ParcelEventType.PICKED_UP))
                .isEqualTo(RemediationDecision.SAFE_TO_AUTO);
    }

    @Test
    void eventThatSkipsAheadNeedsReview() {
        assertThat(gate.decide(ParcelState.PICKED_UP, ParcelEventType.DELIVERED))
                .isEqualTo(RemediationDecision.NEEDS_REVIEW);
    }

    @Test
    void anythingInvolvingAnExceptionNeedsReview() {
        assertThat(gate.decide(ParcelState.EXCEPTION, ParcelEventType.DELIVERED))
                .isEqualTo(RemediationDecision.NEEDS_REVIEW);
        assertThat(gate.decide(ParcelState.DELIVERED, ParcelEventType.EXCEPTION))
                .isEqualTo(RemediationDecision.NEEDS_REVIEW);
    }
}
