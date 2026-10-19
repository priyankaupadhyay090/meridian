package com.meridian.parcel.adapter.messaging;

import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@ConditionalOnProperty(name = "meridian.kafka.enabled", havingValue = "true")
public class SimulationController {

    private final CourierSimulator simulator;

    public SimulationController(CourierSimulator simulator) {
        this.simulator = simulator;
    }

    @PostMapping("/api/console/simulations")
    @ResponseStatus(HttpStatus.ACCEPTED)
    SimulationReport start(@Valid @RequestBody SimulationRequest request, Principal user) {
        return simulator.start(request, user.getName());
    }
}
