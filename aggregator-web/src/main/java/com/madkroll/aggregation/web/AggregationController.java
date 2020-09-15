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

import java.util.Set;
import java.util.concurrent.CompletableFuture;

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
    public CompletableFuture<AggregationResponse> handleAggregateRequest(
            @RequestParam(name = "pricing", required = false) final Set<String> pricing,
            @RequestParam(name = "track", required = false) final Set<String> track,
            @RequestParam(name = "shipments", required = false) final Set<String> shipments
    ) {
        log.debug("pricing: {}, track: {}, shipments: {}", pricing, track, shipments);
        return aggregationService.computeAndMerge(pricing, track, shipments);
    }
}
