package com.madkroll.aggregation.services;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Log4j2
@AllArgsConstructor
public class BatchProcessor<V> {

    private final DataProviderClient<V> dataProviderClient;

    /**
     * Processes single batch:
     * - collects queries
     * - retrieves data per query
     * - submits received responses into corresponding completable future
     */
    public CompletableFuture<Void> process(final List<CompletableFuture<QueryComputation<V>>> completableFutures) {
        if (CollectionUtils.isEmpty(completableFutures)) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture
                // wait until batch is full or timeout happened
                .allOf(completableFutures.toArray(new CompletableFuture[0]))
                .thenApplyAsync(
                        whenNextBatchAvailable -> {
                            // collect all queries from batch
                            return completableFutures.stream()
                                    .map(future -> future.getNow(null))
                                    .filter(Objects::nonNull)
                                    .map(QueryComputation::getQuery)
                                    .collect(Collectors.toList());
                        }
                )
                .thenComposeAsync(
                        queries ->
                                dataProviderClient
                                        .retrieveByHttp(queries)
                                        .toFuture()
                )
                .exceptionally(throwable -> Map.of())
                .thenAcceptAsync(
                        // parse response into map
                        // complete value to corresponding future
                        response ->
                                completableFutures.stream()
                                        .map(future -> future.getNow(null))
                                        .filter(Objects::nonNull)
                                        .forEach(
                                                computation ->
                                                        computation.getResponseCalculation()
                                                                .complete(response.get(computation.getQuery()))
                                        )
                );
    }
}
