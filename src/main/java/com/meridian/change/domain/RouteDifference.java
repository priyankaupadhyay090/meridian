package com.meridian.change.domain;

import com.meridian.routing.domain.CarrierService;

public record RouteDifference(String shipmentId, CarrierService before, CarrierService after) {
}
