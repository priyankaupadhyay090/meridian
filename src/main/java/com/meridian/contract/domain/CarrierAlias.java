package com.meridian.contract.domain;

import com.meridian.routing.domain.CarrierService;

public record CarrierAlias(String phrase, CarrierService service) {
}
