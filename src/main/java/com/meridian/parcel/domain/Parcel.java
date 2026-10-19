package com.meridian.parcel.domain;

import com.meridian.routing.domain.ShipmentProfile;

public record Parcel(String id, ShipmentProfile profile, ParcelState state, String routedBy, String carrierService) {

    public Parcel withState(ParcelState next) {
        return new Parcel(id, profile, next, routedBy, carrierService);
    }
}
