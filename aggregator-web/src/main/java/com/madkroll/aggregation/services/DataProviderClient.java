package com.madkroll.aggregation.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;

@Log4j2
@AllArgsConstructor
public class DataProviderClient<V> {

    private final int backendTimeoutInSeconds;
    private final int maxRetries;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    /**
     * Retrieves over HTTP data from corresponding data provider.
     * Uses non-blocking API.
     * */
    public Mono<Map<String, V>> retrieveByHttp(final Collection<String> queries) {
        final String joinedQueries = String.join(",", queries);
        log.debug("HTTP: requesting queries:{}", joinedQueries);

        return webClient
                .get()
                .uri("?q=" + joinedQueries)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.max(maxRetries))
                .map(this::parseMap)
                .timeout(Duration.ofSeconds(backendTimeoutInSeconds));
    }

    private Map<String, V> parseMap(final String content) {
        try {
            return objectMapper.readValue(content, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to parse response. Response is malformed.", e);
        }
    }
}
