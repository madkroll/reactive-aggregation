package com.madkroll.aggregation.services;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Log4j2
@AllArgsConstructor
public class DataProvider<V> {

    // submit chunks with size not more than batch capacity
    private final int batchCapacity;

    // complete computation anyway once timeout passed and no result yet
    private final int queryComputationTimeoutInSeconds;

    // service managing batch buffering and processing
    private final BatchBufferingService<QueryComputation<V>> batchBufferingService;

    // once batch is ready, handles it's further processing
    private final BatchProcessor<V> batchProcessor;

    /**
     * Asynchronously retrieving data from corresponding data provider.
     */
    public List<QueryComputation<V>> fetch(final Set<String> queries) {
        if (CollectionUtils.isEmpty(queries)) {
            return List.of();
        }

        final List<QueryComputation<V>> computations = toComputations(queries);

        // group by batches asynchronously and release this thread
        Lists.partition(computations, batchCapacity)
                .forEach(
                        nextChunk ->
                                CompletableFuture
                                        .supplyAsync(() -> batchBufferingService.prepareBatch(nextChunk))
                                        .thenAcceptAsync(batchProcessor::process)
                );

        return computations;
    }

    /**
     * Convert incoming queries into computations providing asynchronous access to future results.
     */
    private List<QueryComputation<V>> toComputations(final Set<String> queries) {
        return queries.stream()
                .map(
                        query -> new QueryComputation<>(
                                query,
                                new CompletableFuture<V>(),
                                queryComputationTimeoutInSeconds
                        )
                )
                .collect(Collectors.toList());
    }
}
