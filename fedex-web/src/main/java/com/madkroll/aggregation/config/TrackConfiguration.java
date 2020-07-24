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

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.madkroll.aggregation.utils.FluxUtils.fluxFromQueue;

@Configuration
public class TrackConfiguration {

    @Bean
    public Queue<String> trackQueryQueue() {
        return new ConcurrentLinkedQueue<>();
    }

    @Bean
    public Flux<List<String>> trackFlux(
            final Queue<String> trackQueryQueue,
            final @Value("${providers.track.batch.timeout-in-seconds}") int batchTimeoutInSeconds,
            final @Value("${providers.track.batch.capacity}") int batchCapacity
    ) {
        return fluxFromQueue(trackQueryQueue, batchTimeoutInSeconds, batchCapacity);
    }

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
    public DataProvider<String> trackProvider(
            final @Value("${api.timeout-in-seconds}") int apiTimeoutInSeconds,
            final DataProviderClient<String> trackProviderClient,
            final Queue<String> trackQueryQueue,
            final Flux<List<String>> trackFlux,
            final NoResponseMapper noResponseMapper
    ) {
        return new DataProvider<>(
                apiTimeoutInSeconds,
                trackProviderClient,
                trackQueryQueue,
                trackFlux,
                noResponseMapper
        );
    }
}
