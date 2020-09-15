package com.madkroll.aggregation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
@ToString
public class AggregationResponse {

    private final Map<String, BigDecimal> pricing;
    private final Map<String, String> track;
    private final Map<String, List<String>> shipments;
}
