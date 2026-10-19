package com.meridian.routing.adapter.web;

import com.meridian.common.NotFoundException;
import com.meridian.routing.application.RoutingService;
import com.meridian.routing.domain.RoutingDecision;
import com.meridian.routing.domain.RuleSet;
import com.meridian.routing.domain.ShipmentProfile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/console")
public class RoutingController {

    private final RoutingService routing;

    public RoutingController(RoutingService routing) {
        this.routing = routing;
    }

    @GetMapping("/rules")
    RuleSet activeRules() {
        return routing.activeRules();
    }

    @GetMapping("/rules/history")
    List<RuleSet> history() {
        return routing.history();
    }

    @PostMapping("/routes/resolve")
    RoutingDecision resolve(@RequestBody ShipmentProfile profile) {
        return routing.route(profile)
                .orElseThrow(() -> new NotFoundException("route for destination", profile.destinationPostcode()));
    }
}
