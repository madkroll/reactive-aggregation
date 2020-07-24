package com.madkroll.aggregation.services;

import com.madkroll.aggregation.dto.AggregationResponse;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Log4j2
@Service
@AllArgsConstructor
public class AggregationService {

    private final DataProvider<BigDecimal> pricingProvider;
    private final DataProvider<String> trackProvider;
    private final DataProvider<List<String>> shipmentsProvider;
    private final NoResponseMapper noResponseMapper;

    /**
     * Submits asynchronous data retrieving to all data providers in parallel and awaits for results.
     * Then merges what is available into single response.
     */
    public CompletableFuture<AggregationResponse> computeAndMerge(
            final Set<String> pricing,
            final Set<String> track,
            final Set<String> shipments
    ) {
        // completable futures of ongoing computations
        final var computingPricing = getOrNoValuePerEach(pricingProvider.fetch(pricing));
        final var computingTrack = getOrNoValuePerEach(trackProvider.fetch(track));
        final var computingShipments = getOrNoValuePerEach(shipmentsProvider.fetch(shipments));

        return CompletableFuture.allOf(
                computingPricing,
                computingTrack,
                computingShipments
        ).thenApplyAsync(
                buildAggregatedResponse -> new AggregationResponse(
                        getOrNoValuesForAll(computingPricing, pricing),
                        getOrNoValuesForAll(computingTrack, track),
                        getOrNoValuesForAll(computingShipments, shipments)
                )
        );
    }

    /**
     * Asynchronously awaits for all computation results.
     * Once all results are available, converts them into response object.
     */
    private <V> CompletableFuture<Map<String, V>> getOrNoValuePerEach(final List<QueryComputation<V>> queryComputations) {
        return CompletableFuture.allOf(
                queryComputations.stream()
                        .map(QueryComputation::getResponseHandling)
                        .toArray(CompletableFuture[]::new)
        )
                .thenApplyAsync(
                        buildBatchResponse -> {
                            final Map<String, V> response = new HashMap<>();

                            for (var computation : queryComputations) {
                                response.putIfAbsent(
                                        computation.getQuery(),
                                        computation
                                                .getResponseHandling()
                                                .getNow(null)
                                );
                            }

                            return response;
                        }
                ).exceptionally(
                        // handles global exceptions, local computation-specific exceptions handled on provider level
                        throwable -> noResponseMapper.noResponseMap(
                                queryComputations.stream().map(QueryComputation::getQuery).collect(Collectors.toList())
                        )
                );
    }

    /**
     * Attempts to get completed value.
     * Otherwise returns map with empty values for all original queries.
     * <p>
     * In practice when this method is called - all futures are already completed (thanks to allOf()).
     * So this code is only to comply to proper compilation error handling and most probably is not reachable.
     */
    private <V> Map<String, V> getOrNoValuesForAll(
            final CompletableFuture<Map<String, V>> computingResponses,
            final Collection<String> queries
    ) {
        try {
            return computingResponses.get();
        } catch (Throwable throwable) {
            log.error("Unable to resolve completed values, returning empty response for queries: {}", queries);
            return noResponseMapper.noResponseMap(queries);
        }
    }
}
