package com.madkroll.aggregation.services;

import com.madkroll.aggregation.dto.AggregationResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@AllArgsConstructor
public class AggregationService {

    private final DataProvider<BigDecimal> pricingService;
    private final DataProvider<String> trackService;
    private final DataProvider<List<String>> shipmentsProvider;
    private final NoResponseMapper noResponseMapper;

    public Mono<AggregationResponse> computeAndMerge(
            final List<String> pricing,
            final List<String> track,
            final List<String> shipments) {
        return Mono.zipDelayError(
                pricingService.fetch(pricing),
                trackService.fetch(track),
                shipmentsProvider.fetch(shipments)
        ).flatMap(
                responses -> Mono.just(
                        new AggregationResponse(responses.getT1(), responses.getT2(), responses.getT3())
                )
        ).onErrorResume(throwable ->
                Mono.just(
                        new AggregationResponse(
                                noResponseMapper.noResponseMap(pricing),
                                noResponseMapper.noResponseMap(track),
                                noResponseMapper.noResponseMap(shipments))
                )
        );
    }
}
