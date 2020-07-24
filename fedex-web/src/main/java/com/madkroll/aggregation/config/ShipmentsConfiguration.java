package com.madkroll.aggregation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madkroll.aggregation.services.DataProvider;
import com.madkroll.aggregation.services.DataProviderClient;
import com.madkroll.aggregation.services.NoResponseMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.madkroll.aggregation.utils.FluxUtils.fluxFromQueue;

@Configuration
public class ShipmentsConfiguration {

    @Bean
    public Queue<String> shipmentsQueryQueue() {
        return new ConcurrentLinkedQueue<>();
    }

    @Bean
    public Flux<List<String>> shipmentsFlux(
            final Queue<String> shipmentsQueryQueue,
            final @Value("${providers.shipments.batch.timeout-in-seconds}") int batchTimeoutInSeconds,
            final @Value("${providers.shipments.batch.capacity}") int batchCapacity
    ) {
        return fluxFromQueue(shipmentsQueryQueue, batchTimeoutInSeconds, batchCapacity);
    }

    @Bean
    public WebClient shipmentsWebClient(final @Value("${providers.shipments.backend.base-url}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    public DataProviderClient<List<String>> shipmentsProviderClient(
            final @Value("${providers.shipments.backend.timeout-in-seconds}") int backendTimeoutInSeconds,
            final @Value("${providers.shipments.backend.max-retries}") int maxRetries,
            final WebClient shipmentsWebClient,
            final ObjectMapper objectMapper
    ) {
        return new DataProviderClient<>(
                backendTimeoutInSeconds,
                maxRetries,
                shipmentsWebClient,
                objectMapper
        );
    }

    @Bean
    public DataProvider<List<String>> shipmentsProvider(
            final @Value("${api.timeout-in-seconds}") int apiTimeoutInSeconds,
            final DataProviderClient<List<String>> shipmentsProviderClient,
            final Queue<String> shipmentsQueryQueue,
            @Qualifier("shipmentsFlux") final Flux<List<String>> shipmentsFlux,
            final NoResponseMapper noResponseMapper
    ) {
        return new DataProvider<>(
                apiTimeoutInSeconds,
                shipmentsProviderClient,
                shipmentsQueryQueue,
                shipmentsFlux,
                noResponseMapper
        );
    }
}
