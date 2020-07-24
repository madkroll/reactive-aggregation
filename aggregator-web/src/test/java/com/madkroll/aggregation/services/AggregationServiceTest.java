package com.madkroll.aggregation.services;

import com.madkroll.aggregation.dto.AggregationResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@RunWith(MockitoJUnitRunner.class)
public class AggregationServiceTest {

    private static final Set<String> PRICING_QUERIES = Set.of("pricing-key-1", "pricing-key-2");
    private static final Set<String> TRACK_QUERIES = Set.of("track-key-1", "track-key-2");
    private static final Set<String> SHIPMENTS_QUERIES = Set.of("shipments-key-1", "shipments-key-2");

    private static final Set<String> TRACK_NO_QUERIES = Set.of();
    private static final Set<String> SHIPMENTS_NO_QUERIES = Set.of();

    private static final Map<String, BigDecimal> DEFAULT_PRICING_VALUES_OK =
            Map.of(
                    "pricing-key-1", BigDecimal.ONE,
                    "pricing-key-2", BigDecimal.TEN
            );

    private static final Map<String, String> DEFAULT_TRACK_VALUES_OK =
            Map.of(
                    "track-key-1", "track-value-1",
                    "track-key-2", "track-value-2"
            );

    private static final Map<String, List<String>> DEFAULT_SHIPMENTS_VALUES_OK =
            Map.of(
                    "shipments-key-1", List.of("shipments-value-1-1", "shipments-value-1-2"),
                    "shipments-key-2", List.of("shipments-value-2-1", "shipments-value-2-2")
            );

    @Mock
    private DataProvider<BigDecimal> pricingProvider;

    @Mock
    private DataProvider<String> trackProvider;

    @Mock
    private DataProvider<List<String>> shipmentsProvider;

    @Mock
    private NoResponseMapper noResponseMapper;

    private AggregationService aggregationService;

    @Before
    public void setUp() {
        aggregationService = new AggregationService(pricingProvider, trackProvider, shipmentsProvider, noResponseMapper);
    }

    @Test
    public void shouldCompleteOnlyOnceAllCompleted() throws Exception {
        // given
        final List<QueryComputation<BigDecimal>> priceComputations = mockProvider(pricingProvider, PRICING_QUERIES);
        final List<QueryComputation<String>> trackComputations = mockProvider(trackProvider, TRACK_QUERIES);
        final List<QueryComputation<List<String>>> shipmentsComputations = mockProvider(shipmentsProvider, SHIPMENTS_QUERIES);

        // when
        final CompletableFuture<AggregationResponse> resultComputation = aggregationService.computeAndMerge(
                PRICING_QUERIES, TRACK_QUERIES, SHIPMENTS_QUERIES
        );

        // then
        assertThat(resultComputation.isDone()).isFalse();
        // even when
        completeAllOK(priceComputations, DEFAULT_PRICING_VALUES_OK);
        assertThat(resultComputation.isDone()).isFalse();
        // even when
        completeAllOK(trackComputations, DEFAULT_TRACK_VALUES_OK);
        assertThat(resultComputation.isDone()).isFalse();
        // and only when
        completeAllOK(shipmentsComputations, DEFAULT_SHIPMENTS_VALUES_OK);
        // then
        assertThat(
                resultComputation
                        // secure async execution
                        .orTimeout(20, TimeUnit.MILLISECONDS)
                        .get()
        )
                .usingRecursiveComparison()
                .isEqualTo(new AggregationResponse(DEFAULT_PRICING_VALUES_OK, DEFAULT_TRACK_VALUES_OK, DEFAULT_SHIPMENTS_VALUES_OK));

        verifyNoInteractions(noResponseMapper);
    }

    @Test
    public void shouldCompleteEvenWhenOnlyOneProviderResponds() throws Exception {
        // given
        final List<QueryComputation<BigDecimal>> priceComputations = mockProvider(pricingProvider, PRICING_QUERIES);

        // when
        final CompletableFuture<AggregationResponse> resultComputation = aggregationService.computeAndMerge(
                PRICING_QUERIES,
                TRACK_NO_QUERIES, SHIPMENTS_NO_QUERIES
        );

        // then
        assertThat(resultComputation.isDone()).isFalse();
        // and only when
        completeAllOK(priceComputations, DEFAULT_PRICING_VALUES_OK);
        assertThat(
                resultComputation
                        // secure async execution
                        .orTimeout(20, TimeUnit.MILLISECONDS)
                        .get()
        )
                .usingRecursiveComparison()
                .isEqualTo(new AggregationResponse(DEFAULT_PRICING_VALUES_OK, Map.of(), Map.of()));

        verifyNoInteractions(noResponseMapper);
    }

    @Test
    public void shouldReturnNullValuesPerEachFailedComputation() throws Exception {
        // given
        final List<QueryComputation<BigDecimal>> priceComputations = mockProvider(pricingProvider, PRICING_QUERIES);
        final QueryComputation<BigDecimal> okComputation = priceComputations.get(0);
        final QueryComputation<BigDecimal> failedComputation = priceComputations.get(1);

        // when
        final CompletableFuture<AggregationResponse> resultComputation = aggregationService.computeAndMerge(
                PRICING_QUERIES,
                TRACK_NO_QUERIES, SHIPMENTS_NO_QUERIES
        );

        final Map<String, BigDecimal> expectedResponse = new HashMap<>();
        expectedResponse.put(okComputation.getQuery(), DEFAULT_PRICING_VALUES_OK.get(okComputation.getQuery()));
        expectedResponse.put(failedComputation.getQuery(), null);

        // then
        assertThat(resultComputation.isDone()).isFalse();
        // and only when
        completeOK(okComputation, DEFAULT_PRICING_VALUES_OK.get(okComputation.getQuery()));
        failedComputation.getResponseCalculation().completeExceptionally(new IllegalStateException());
        // then
        assertThat(
                resultComputation
                        // secure async execution
                        .orTimeout(20, TimeUnit.MILLISECONDS)
                        .get()
        )
                .usingRecursiveComparison()
                .isEqualTo(
                        new AggregationResponse(
                                expectedResponse, Map.of(), Map.of()
                        )
                );
    }

    private <V> List<QueryComputation<V>> mockProvider(final DataProvider<V> provider, final Set<String> queries) {
        final List<QueryComputation<V>> computations = queries.stream()
                .map(query -> new QueryComputation<V>(query, new CompletableFuture<>(), 10))
                .collect(Collectors.toList());

        given(provider.fetch(queries)).willReturn(computations);

        return computations;
    }

    private <V> void completeAllOK(final List<QueryComputation<V>> computations, final Map<String, V> okValues) {
        computations.forEach(
                computation -> computation.getResponseCalculation().complete(okValues.get(computation.getQuery()))
        );
    }

    private <V> void completeOK(final QueryComputation<V> computation, final V value) {
        computation.getResponseCalculation().complete(value);
    }

    private <V> Map<String, V> mapAllToNull(final Collection<String> keys) {
        final Map<String, V> result = new HashMap<>();

        keys.forEach(
                key -> result.putIfAbsent(key, null)
        );

        return result;
    }
}