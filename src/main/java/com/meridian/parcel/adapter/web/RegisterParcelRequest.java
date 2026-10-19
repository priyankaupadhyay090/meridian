package com.meridian.parcel.adapter.web;

import com.meridian.routing.domain.ShipmentProfile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterParcelRequest(@NotBlank String id, @NotNull ShipmentProfile profile) {
}
