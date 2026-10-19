package com.meridian.contract.adapter.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.contract.domain.ClauseCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class ClauseCatalogConfig {

    @Bean
    ClauseCatalog clauseCatalog(ObjectMapper mapper) throws IOException {
        try (InputStream in = new ClassPathResource("seed/clause-catalog.json").getInputStream()) {
            return mapper.readValue(in, ClauseCatalog.class);
        }
    }
}
