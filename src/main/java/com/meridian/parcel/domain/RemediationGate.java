package com.meridian.parcel.domain;

public class RemediationGate {

    public RemediationDecision decide(ParcelState current, ParcelEventType rejectedEvent) {
        ParcelState claimed = ParcelState.after(rejectedEvent);
        boolean bothInLifecycle = current != ParcelState.EXCEPTION && claimed != ParcelState.EXCEPTION;
        if (bothInLifecycle && claimed.stage() < current.stage()) {
            return RemediationDecision.SAFE_TO_AUTO;
        }
        return RemediationDecision.NEEDS_REVIEW;
    }
}
