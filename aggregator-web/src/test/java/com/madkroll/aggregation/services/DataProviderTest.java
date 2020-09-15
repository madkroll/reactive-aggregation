package com.madkroll.aggregation.services;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DataProviderTest {

    private static final String QUERY = "1";
    private static final Set<String> QUERIES = Set.of("1", "2", "3");

    private static final int BATCH_CAPACITY = 2;
    private static final int COMPUTATION_TIMEOUT_IN_SECONDS = 1;

    @Mock
    private BatchBufferingService<QueryComputation<String>> batchBufferingService;

    @Mock
    private BatchProcessor<String> batchProcessor;

    @Test
    public void shouldReturnNoComputationsIfNothingQueried() {
        assertThat(
                new DataProvider<>(BATCH_CAPACITY, COMPUTATION_TIMEOUT_IN_SECONDS, batchBufferingService, batchProcessor)
                        .fetch(Set.of())
        ).isEmpty();
    }

    @Test
    public void shouldCompleteWithNullValueIfTimeoutReached() throws Exception {
        final List<QueryComputation<String>> computations =
                new DataProvider<>(BATCH_CAPACITY, COMPUTATION_TIMEOUT_IN_SECONDS, batchBufferingService, batchProcessor)
                        .fetch(Set.of(QUERY));

        assertThat(computations).hasSize(1);
        assertThat(
                computations.get(0)
                        .getResponseCalculation()
                        .get(COMPUTATION_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
        ).isNull();
    }

    @Test
    public void shouldSpitInTwoChunks() throws Exception {
        new DataProvider<>(BATCH_CAPACITY, COMPUTATION_TIMEOUT_IN_SECONDS, batchBufferingService, batchProcessor)
                .fetch(QUERIES);
        Thread.sleep(1000);
        verify(batchBufferingService, times(2)).prepareBatch(anyList());
        verify(batchProcessor, times(2)).process(anyList());
    }

    @Test
    public void shouldCompleteWithNullValueIfExceptionallyCompleted() throws Exception {
        final List<QueryComputation<String>> computations =
                new DataProvider<>(BATCH_CAPACITY, COMPUTATION_TIMEOUT_IN_SECONDS, batchBufferingService, batchProcessor)
                        .fetch(Set.of(QUERY));

        assertThat(computations).hasSize(1);

        computations.get(0)
                .getResponseCalculation()
                .completeExceptionally(new IllegalStateException());

        assertThat(
                computations.get(0)
                        .getResponseHandling()
                        .get()
        ).isNull();
    }
}