package com.meridian.delivery.domain;

public enum Courier {
    DHL("DHL"),
    HERMES("Hermes"),
    DPD("DPD"),
    GLS("GLS");

    private final String displayName;

    Courier(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
