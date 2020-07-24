package com.madkroll.aggregation.web;

import com.madkroll.aggregation.dto.AggregationResponse;
import com.madkroll.aggregation.services.AggregationService;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import reactor.core.publisher.Mono;

import java.util.List;

@Log4j2
@AllArgsConstructor
@RequestMapping
@RestController
@EnableWebMvc
public class AggregationController {

    private final AggregationService aggregationService;

    @GetMapping(
            path = "/aggregation",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<AggregationResponse> handleAggregateRequest(
            @RequestParam(name = "pricing", required = false) final List<String> pricing,
            @RequestParam(name = "track", required = false) final List<String> track,
            @RequestParam(name = "shipments", required = false) final List<String> shipments
    ) {
        log.info("pricing: {}, track: {}, shipments: {}", pricing, track, shipments);
        return aggregationService.computeAndMerge(pricing, track, shipments);
    }
}
