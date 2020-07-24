package com.madkroll.aggregation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madkroll.aggregation.services.DataProvider;
import com.madkroll.aggregation.services.DataProviderClient;
import com.madkroll.aggregation.services.NoResponseMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.madkroll.aggregation.utils.FluxUtils.fluxFromQueue;

@Configuration
public class PricingConfiguration {

    @Bean
    public Queue<String> pricingQueryQueue() {
        return new ConcurrentLinkedQueue<>();
    }

    @Bean
    public Flux<List<String>> pricingFlux(
            final Queue<String> pricingQueryQueue,
            final @Value("${providers.pricing.batch.timeout-in-seconds}") int batchTimeoutInSeconds,
            final @Value("${providers.pricing.batch.capacity}") int batchCapacity
    ) {
        return fluxFromQueue(pricingQueryQueue, batchTimeoutInSeconds, batchCapacity);
    }

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
    public DataProvider<BigDecimal> pricingProvider(
            final @Value("${api.timeout-in-seconds}") int apiTimeoutInSeconds,
            final DataProviderClient<BigDecimal> pricingProviderClient,
            final Queue<String> pricingQueryQueue,
            final Flux<List<String>> pricingFlux,
            final NoResponseMapper noResponseMapper
    ) {
        return new DataProvider<>(
                apiTimeoutInSeconds,
                pricingProviderClient,
                pricingQueryQueue,
                pricingFlux,
                noResponseMapper
        );
    }
}
