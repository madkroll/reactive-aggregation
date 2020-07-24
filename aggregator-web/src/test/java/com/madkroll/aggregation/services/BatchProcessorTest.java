package com.madkroll.aggregation.services;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@RunWith(MockitoJUnitRunner.class)
public class BatchProcessorTest {

    private static final String QUERY = "query";
    private static final String VALUE = "value";

    @Mock
    private DataProviderClient<String> dataProviderClient;

    @Test
    public void shouldProcessNonNullItemsInBatch() throws Exception {
        // given
        final var oneEmptyOneWithValue = newBatch();
        final var emptySlot = oneEmptyOneWithValue.get(0);
        final var slotWithValue = oneEmptyOneWithValue.get(1);
        final var valueCalculation = new CompletableFuture<String>();
        given(dataProviderClient.retrieveByHttp(argThat(queries -> queries.size() == 1 && queries.contains(QUERY))))
                .willReturn(Mono.just(Map.of(QUERY, VALUE)));

        // when
        final CompletableFuture<Void> runningComputations = new BatchProcessor<>(dataProviderClient).process(oneEmptyOneWithValue);
        emptySlot.complete(null);
        slotWithValue.complete(new QueryComputation<>(QUERY, valueCalculation, 10));

        // wait all chain is processed
        runningComputations.get(1, TimeUnit.SECONDS);
        assertThat(runningComputations).isCompleted();
        assertThat(slotWithValue.get().getResponseCalculation().get()).isEqualTo(VALUE);
    }

    @Test
    public void shouldReturnNullIfClientCompletesExceptionally() throws Exception {
        // given
        final var oneWithValue = List.of(new CompletableFuture<QueryComputation<String>>());
        final var slotWithValue = oneWithValue.get(0);
        final var valueCalculation = new CompletableFuture<String>();
        given(dataProviderClient.retrieveByHttp(argThat(queries -> queries.size() == 1 && queries.contains(QUERY))))
                .willReturn(Mono.error(IllegalStateException::new));

        // when
        final CompletableFuture<Void> runningComputations = new BatchProcessor<>(dataProviderClient).process(oneWithValue);
        slotWithValue.complete(new QueryComputation<>(QUERY, valueCalculation, 10));

        // wait all chain is processed
        runningComputations.get(1, TimeUnit.SECONDS);
        assertThat(runningComputations).isCompleted();
        assertThat(slotWithValue.get().getResponseCalculation().get()).isNull();
    }

    @Test
    public void shouldReturnNullIfClientFailed() throws Exception {
        // given
        final var oneWithValue = List.of(new CompletableFuture<QueryComputation<String>>());
        final var slotWithValue = oneWithValue.get(0);
        final var valueCalculation = new CompletableFuture<String>();
        given(dataProviderClient.retrieveByHttp(argThat(queries -> queries.size() == 1 && queries.contains(QUERY))))
                .willThrow(new IllegalStateException());

        // when
        final CompletableFuture<Void> runningComputations = new BatchProcessor<>(dataProviderClient).process(oneWithValue);
        slotWithValue.complete(new QueryComputation<>(QUERY, valueCalculation, 10));

        // wait all chain is processed
        runningComputations.get(1, TimeUnit.SECONDS);
        assertThat(runningComputations).isCompleted();
        assertThat(slotWithValue.get().getResponseCalculation().get()).isNull();
    }

    @Test
    public void shouldNotProcessIfBatchIsEmpty() throws Exception {
        final var result = new BatchProcessor<>(dataProviderClient).process(List.of());

        result.get(1, TimeUnit.SECONDS);
        assertThat(result).isCompleted();
        verifyNoInteractions(dataProviderClient);
    }

    private List<CompletableFuture<QueryComputation<String>>> newBatch() {
        return List.of(
                new CompletableFuture<>(),
                new CompletableFuture<>()
        );
    }
}