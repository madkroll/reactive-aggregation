package com.madkroll.aggregation.services;

import lombok.Getter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Getter
public class QueryComputation<V> {

    private final String query;
    private final CompletableFuture<V> responseCalculation;
    private final CompletableFuture<V> responseHandling;

    public QueryComputation(
            final String query,
            final CompletableFuture<V> responseCalculation,
            final int timeoutInSeconds
    ) {
        this.query = query;
        this.responseCalculation = responseCalculation;
        this.responseHandling =
                responseCalculation
                        .completeOnTimeout(null, timeoutInSeconds, TimeUnit.SECONDS)
                        .exceptionally(throwable -> null);
    }
}
