package com.meridian.parcel.adapter.messaging;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SimulationRequest(@Min(1) @Max(5_000) int parcels, @DecimalMin("0") @DecimalMax("0.5") double faultShare) {
}
