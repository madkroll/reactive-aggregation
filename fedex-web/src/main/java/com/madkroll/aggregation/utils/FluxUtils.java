package com.madkroll.aggregation.utils;

import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Queue;

public final class FluxUtils {

    public static <T> Flux<List<T>> fluxFromQueue(
            final Queue<T> queue,
            final int batchTimeoutInSeconds,
            final int batchCapacity
            ) {
        return Flux.<T>generate(
                sink -> {
                    final T nextQuery = queue.poll();

                    if (nextQuery == null) {
                        sink.complete();
                    } else {
                        sink.next(nextQuery);
                    }
                }
        )
                .log()
                .repeatWhen(it -> it.delayElements(Duration.ofMillis(10)))
//                .distinct() // TODO check if needed
                .bufferTimeout(batchCapacity, Duration.ofSeconds(batchTimeoutInSeconds))
                .publish()
                .autoConnect();
    }
}
