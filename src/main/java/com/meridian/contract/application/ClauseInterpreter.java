package com.meridian.contract.application;

import com.meridian.contract.domain.Clause;
import com.meridian.contract.domain.ClauseInterpretation;

import java.util.List;
import java.util.Optional;

public interface ClauseInterpreter {

    ClauseInterpreter NONE = new ClauseInterpreter() {
        @Override
        public Optional<ClauseInterpretation> interpret(Clause clause, List<String> carrierServices) {
            return Optional.empty();
        }

        @Override
        public String name() {
            return "none";
        }
    };

    Optional<ClauseInterpretation> interpret(Clause clause, List<String> carrierServices);

    String name();

    default boolean enabled() {
        return !name().equals("none");
    }
}
