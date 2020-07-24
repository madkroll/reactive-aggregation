package com.madkroll.aggregation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madkroll.aggregation.services.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

@Configuration
public class ShipmentsConfiguration {

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
    public BatchProcessor<List<String>> shipmentsBatchProcessor(
            final DataProviderClient<List<String>> shipmentsProviderClient
    ) {
        return new BatchProcessor<>(shipmentsProviderClient);
    }

    @Bean
    public DataProvider<List<String>> shipmentsProvider(
            final @Value("${providers.track.batch.capacity}") int batchCapacity,
            final @Value("${providers.track.backend.timeout-in-seconds}") int computationTimeoutInSeconds,
            final BatchProcessor<List<String>> shipmentsBatchProcessor,
            final BatchBufferingService<QueryComputation<List<String>>> shipmentsBatchBufferingService
    ) {
        return new DataProvider<>(
                batchCapacity,
                computationTimeoutInSeconds,
                shipmentsBatchBufferingService,
                shipmentsBatchProcessor
        );
    }

    @Bean
    public Queue<CompletableFuture<QueryComputation<List<String>>>> shipmentsBatchBuffer() {
        return new LinkedBlockingQueue<>();
    }

    @Bean
    public BatchBufferingService<QueryComputation<List<String>>> shipmentsBatchBufferingService(
            final @Value("${providers.shipments.batch.capacity}") int batchCapacity,
            final @Value("${providers.shipments.batch.timeout-in-seconds}") int batchTimeoutInSeconds,
            final Queue<CompletableFuture<QueryComputation<List<String>>>> shipmentsBatchBuffer
    ) {
        return new BatchBufferingService<>(
                batchCapacity,
                batchTimeoutInSeconds,
                shipmentsBatchBuffer
        );
    }
}
