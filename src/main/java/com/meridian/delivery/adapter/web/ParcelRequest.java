package com.meridian.delivery.adapter.web;

import com.meridian.delivery.domain.Parcel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ParcelRequest(@Min(1) @Max(70_000) int weightGrams,
                            @Min(1) @Max(300) int lengthCm,
                            @Min(1) @Max(300) int widthCm,
                            @Min(1) @Max(300) int heightCm) {

    Parcel toParcel() {
        return new Parcel(weightGrams, lengthCm, widthCm, heightCm);
    }
}
