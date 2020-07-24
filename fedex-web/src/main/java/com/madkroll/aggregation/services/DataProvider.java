package com.madkroll.aggregation.services;

import lombok.AllArgsConstructor;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Queue;

@AllArgsConstructor
public class DataProvider<V> {

    private final int timeoutInSeconds;
    private final DataProviderClient<V> dataProviderClient;
    private final Queue<String> queue;
    private final Flux<List<String>> fluxProcessingQueue;
    private final NoResponseMapper noResponseMapper;

    public Mono<Map<String, V>> fetch(final List<String> queries) {
        if (CollectionUtils.isEmpty(queries)) {
            return Mono.just(Map.of());
        }

        queue.addAll(queries);

        return fluxProcessingQueue
                .log()
                .publishOn(Schedulers.elastic())
                .flatMap(dataProviderClient::retrieveByHttp)
                .publishOn(Schedulers.parallel())
                .flatMap(response -> Flux.fromIterable(response.entrySet()))
                .filter(nextEntry -> queries.contains(nextEntry.getKey()))
                .distinct(Map.Entry::getKey)
                .take(queries.size())
                .timeout(Duration.ofSeconds(timeoutInSeconds))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .onErrorResume(throwable -> Mono.just(noResponseMapper.noResponseMap(queries)));
    }
}
