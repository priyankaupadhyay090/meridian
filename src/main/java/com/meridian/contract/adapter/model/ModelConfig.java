package com.meridian.contract.adapter.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.contract.application.ClauseInterpreter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class ModelConfig {

    @Bean
    ClauseInterpreter clauseInterpreter(@Value("${meridian.model.provider:none}") String provider,
                                        @Value("${meridian.model.ollama.url:http://localhost:11434}") String url,
                                        @Value("${meridian.model.ollama.model:qwen2.5:7b}") String model,
                                        RestClient.Builder builder, ObjectMapper mapper) {
        if (!provider.equals("ollama")) {
            return ClauseInterpreter.NONE;
        }
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(http);
        requests.setReadTimeout(Duration.ofSeconds(90));
        return new OllamaClauseInterpreter(builder.baseUrl(url).requestFactory(requests).build(), mapper, model);
    }
}
