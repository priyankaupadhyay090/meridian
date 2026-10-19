package com.meridian.parcel.adapter.messaging;

import java.util.Map;

public record SimulationReport(String runId, int parcels, int messages, Map<String, Integer> faults) {
}
