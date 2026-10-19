package com.meridian.delivery.adapter.web;

import com.meridian.delivery.domain.Courier;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record CheckoutOptionsRequest(@Valid @NotNull ParcelRequest parcel,
                                     Courier shopDefault,
                                     @Min(0) @Max(10_000) long shopShippingCents,
                                     @NotEmpty Set<Courier> couriers) {
}
