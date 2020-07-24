package com.madkroll.aggregation.services;

import com.madkroll.aggregation.dto.AggregationResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class AggregationServiceTest {

    private static final List<String> PRICING_QUERIES = List.of("pricing-key");
    private static final List<String> TRACK_QUERIES = List.of("track-key");
    private static final List<String> SHIPMENTS_QUERIES = List.of("shipments-key");

    private static final Map<String, BigDecimal> PRICES_RESPONSE = Map.of("pricing-key", BigDecimal.ONE);
    private static final Map<String, String> TRACK_RESPONSE = Map.of("track-key", "track-value");
    private static final Map<String, List<String>> SHIPMENTS_RESPONSE = Map.of("shipments-key", List.of("shipments-value"));

    private static final Map<String, BigDecimal> PRICES_RESPONSE_WITH_NULL;
    private static final Map<String, String> TRACK_RESPONSE_WITH_NULL;
    private static final Map<String, List<String>> SHIPMENTS_RESPONSE_WITH_NULL;

    static {
        PRICES_RESPONSE_WITH_NULL = new HashMap<>(PRICES_RESPONSE);
        PRICES_RESPONSE_WITH_NULL.replaceAll((key, oldValue) -> null);
        TRACK_RESPONSE_WITH_NULL = new HashMap<>(TRACK_RESPONSE);
        TRACK_RESPONSE_WITH_NULL.replaceAll((key, oldValue) -> null);
        SHIPMENTS_RESPONSE_WITH_NULL = new HashMap<>(SHIPMENTS_RESPONSE);
        SHIPMENTS_RESPONSE_WITH_NULL.replaceAll((key, oldValue) -> null);
    }

    @Mock
    private DataProvider<BigDecimal> pricingService;

    @Mock
    private DataProvider<String> trackService;

    @Mock
    private DataProvider<List<String>> shipmentsProvider;

    @Mock
    private NoResponseMapper noResponseMapper;

    @Test
    public void shouldReturnAggregatedResponses() {
        // given
        given(pricingService.fetch(PRICING_QUERIES)).willReturn(Mono.just(PRICES_RESPONSE));
        given(trackService.fetch(TRACK_QUERIES)).willReturn(Mono.just(TRACK_RESPONSE));
        given(shipmentsProvider.fetch(SHIPMENTS_QUERIES)).willReturn(Mono.just(SHIPMENTS_RESPONSE));

        // when
        final AggregationResponse response =
                new AggregationService(pricingService, trackService, shipmentsProvider, noResponseMapper)
                        .computeAndMerge(PRICING_QUERIES, TRACK_QUERIES, SHIPMENTS_QUERIES).block();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getPricing()).containsExactlyInAnyOrderEntriesOf(PRICES_RESPONSE);
        assertThat(response.getTrack()).containsExactlyInAnyOrderEntriesOf(TRACK_RESPONSE);
        assertThat(response.getShipments()).containsExactlyInAnyOrderEntriesOf(SHIPMENTS_RESPONSE);
    }

    @Test
    public void shouldReturnResponseWithNullsOnlyOnError() {
        // given
        given(pricingService.fetch(PRICING_QUERIES)).willReturn(Mono.error(TimeoutException::new));
        given(trackService.fetch(TRACK_QUERIES)).willReturn(Mono.just(TRACK_RESPONSE));
        given(shipmentsProvider.fetch(SHIPMENTS_QUERIES)).willReturn(Mono.just(SHIPMENTS_RESPONSE));

        given(noResponseMapper.<BigDecimal>noResponseMap(PRICING_QUERIES)).willReturn(PRICES_RESPONSE_WITH_NULL);
        given(noResponseMapper.<String>noResponseMap(TRACK_QUERIES)).willReturn(TRACK_RESPONSE_WITH_NULL);
        given(noResponseMapper.<List<String>>noResponseMap(SHIPMENTS_QUERIES)).willReturn(SHIPMENTS_RESPONSE_WITH_NULL);

        // when
        final AggregationResponse response =
                new AggregationService(pricingService, trackService, shipmentsProvider, noResponseMapper)
                        .computeAndMerge(PRICING_QUERIES, TRACK_QUERIES, SHIPMENTS_QUERIES).block();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getPricing()).containsExactlyInAnyOrderEntriesOf(PRICES_RESPONSE_WITH_NULL);
        assertThat(response.getTrack()).containsExactlyInAnyOrderEntriesOf(TRACK_RESPONSE_WITH_NULL);
        assertThat(response.getShipments()).containsExactlyInAnyOrderEntriesOf(SHIPMENTS_RESPONSE_WITH_NULL);
    }


}