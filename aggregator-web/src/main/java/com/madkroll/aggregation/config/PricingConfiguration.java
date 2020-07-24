package com.madkroll.aggregation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madkroll.aggregation.services.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

@Configuration
public class PricingConfiguration {

    @Bean
    public WebClient pricingWebClient(final @Value("${providers.pricing.backend.base-url}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    public DataProviderClient<BigDecimal> pricingProviderClient(
            final @Value("${providers.pricing.backend.timeout-in-seconds}") int backendTimeoutInSeconds,
            final @Value("${providers.pricing.backend.max-retries}") int maxRetries,
            final WebClient pricingWebClient,
            final ObjectMapper objectMapper
    ) {
        return new DataProviderClient<>(
                backendTimeoutInSeconds,
                maxRetries,
                pricingWebClient,
                objectMapper
        );
    }

    @Bean
    public BatchProcessor<BigDecimal> pricingBatchProcessor(
            final DataProviderClient<BigDecimal> pricingProviderClient
    ) {
        return new BatchProcessor<>(pricingProviderClient);
    }

    @Bean
    public DataProvider<BigDecimal> pricingProvider(
            final @Value("${providers.track.batch.capacity}") int batchCapacity,
            final @Value("${providers.track.backend.timeout-in-seconds}") int computationTimeoutInSeconds,
            final BatchProcessor<BigDecimal> pricingBatchProcessor,
            final BatchBufferingService<QueryComputation<BigDecimal>> pricingBatchBufferingService
    ) {
        return new DataProvider<>(
                batchCapacity,
                computationTimeoutInSeconds,
                pricingBatchBufferingService,
                pricingBatchProcessor
        );
    }

    @Bean
    public Queue<CompletableFuture<QueryComputation<BigDecimal>>> pricingBatchBuffer() {
        return new LinkedBlockingQueue<>();
    }

    @Bean
    public BatchBufferingService<QueryComputation<BigDecimal>> pricingBatchBufferingService(
            final @Value("${providers.pricing.batch.capacity}") int batchCapacity,
            final @Value("${providers.pricing.batch.timeout-in-seconds}") int batchTimeoutInSeconds,
            final Queue<CompletableFuture<QueryComputation<BigDecimal>>> pricingBatchBuffer
    ) {
        return new BatchBufferingService<>(
                batchCapacity,
                batchTimeoutInSeconds,
                pricingBatchBuffer
        );
    }
}
