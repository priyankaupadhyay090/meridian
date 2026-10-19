package com.meridian.routing.domain;

public record ShipmentProfile(String destinationPostcode, ServiceLevel serviceLevel, int weightGrams,
                              CustomerTier customerTier) {
}
