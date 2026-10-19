package com.meridian.routing.adapter.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.common.SeedResources;
import com.meridian.routing.application.RuleSetStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RuleSetStoreConfig {

    @Bean
    RuleSetStore ruleSetStore(ObjectMapper mapper) {
        return new InMemoryRuleSetStore(SeedResources.read(mapper, "seed/rules.json", new TypeReference<>() {
        }));
    }
}
