package com.meridian.routing.application;

import com.meridian.routing.domain.RoutingDecision;
import com.meridian.routing.domain.RuleSet;
import com.meridian.routing.domain.ShipmentProfile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoutingService {

    private final RuleSetStore store;

    public RoutingService(RuleSetStore store) {
        this.store = store;
    }

    public RuleSet activeRules() {
        return store.current();
    }

    public List<RuleSet> history() {
        return store.history();
    }

    public Optional<RoutingDecision> route(ShipmentProfile profile) {
        return store.current().resolve(profile);
    }
}
