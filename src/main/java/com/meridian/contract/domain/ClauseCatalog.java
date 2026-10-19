package com.meridian.contract.domain;

import java.util.List;

public record ClauseCatalog(List<CarrierAlias> carrierServices, List<String> conditionMarkers,
                            List<String> instructionMarkers) {

    public ClauseCatalog {
        carrierServices = List.copyOf(carrierServices);
        conditionMarkers = List.copyOf(conditionMarkers);
        instructionMarkers = List.copyOf(instructionMarkers);
    }
}
