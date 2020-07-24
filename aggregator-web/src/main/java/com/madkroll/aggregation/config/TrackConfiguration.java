package com.madkroll.aggregation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madkroll.aggregation.services.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

@Configuration
public class TrackConfiguration {

    @Bean
    public WebClient trackWebClient(final @Value("${providers.track.backend.base-url}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    public DataProviderClient<String> trackProviderClient(
            final @Value("${providers.track.backend.timeout-in-seconds}") int backendTimeoutInSeconds,
            final @Value("${providers.track.backend.max-retries}") int maxRetries,
            final WebClient trackWebClient,
            final ObjectMapper objectMapper
    ) {
        return new DataProviderClient<>(
                backendTimeoutInSeconds,
                maxRetries,
                trackWebClient,
                objectMapper
        );
    }

    @Bean
    public BatchProcessor<String> trackBatchProcessor(
            final DataProviderClient<String> trackProviderClient
    ) {
        return new BatchProcessor<>(trackProviderClient);
    }

    @Bean
    public DataProvider<String> trackProvider(
            final @Value("${providers.track.batch.capacity}") int batchCapacity,
            final @Value("${providers.track.backend.timeout-in-seconds}") int computationTimeoutInSeconds,
            final BatchProcessor<String> trackBatchProcessor,
            final BatchBufferingService<QueryComputation<String>> trackBatchBufferingService
    ) {
        return new DataProvider<>(
                batchCapacity,
                computationTimeoutInSeconds,
                trackBatchBufferingService,
                trackBatchProcessor
        );
    }

    @Bean
    public Queue<CompletableFuture<QueryComputation<String>>> trackBatchBuffer() {
        return new LinkedBlockingQueue<>();
    }

    @Bean
    public BatchBufferingService<QueryComputation<String>> trackBatchBufferingService(
            final @Value("${providers.track.batch.capacity}") int batchCapacity,
            final @Value("${providers.track.batch.timeout-in-seconds}") int batchTimeoutInSeconds,
            final Queue<CompletableFuture<QueryComputation<String>>> trackBatchBuffer
    ) {
        return new BatchBufferingService<>(
                batchCapacity,
                batchTimeoutInSeconds,
                trackBatchBuffer
        );
    }
}
