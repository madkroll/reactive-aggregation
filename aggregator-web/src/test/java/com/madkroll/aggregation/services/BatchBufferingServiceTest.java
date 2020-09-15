package com.madkroll.aggregation.services;

import org.junit.Test;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;

public class BatchBufferingServiceTest {

    private static final int BATCH_CAPACITY = 3;
    private static final int BATCH_TIMEOUT_IN_SECONDS = 1;
    private static final List<String> CHUNK_FULL = List.of("1", "2", "3");

    @Test
    public void shouldReturnBatchWithAllCompletedWhenCapacityReached() {
        // given
        final LinkedBlockingQueue<CompletableFuture<String>> noSlots = spy(new LinkedBlockingQueue<>());

        // when
        final List<CompletableFuture<String>> completeBatch =
                new BatchBufferingService<>(BATCH_CAPACITY, BATCH_TIMEOUT_IN_SECONDS, noSlots)
                        .prepareBatch(CHUNK_FULL);

        assertThat(completeBatch).hasSize(BATCH_CAPACITY);

        assertThat(
                completeBatch.stream()
                        .map(future -> future.getNow(null))
                        .collect(Collectors.toList())
        ).containsExactlyInAnyOrderElementsOf(CHUNK_FULL);

        verifyNoInteractions(noSlots);
    }

    @Test
    public void shouldFeedOneAndProvideAvailableSlotsLeft() throws Exception {
        // given
        final CompletableFuture<String> availableSlot = new CompletableFuture<>();
        final LinkedBlockingQueue<CompletableFuture<String>> oneSlotIsAvailable =
                spy(new LinkedBlockingQueue<>(List.of(availableSlot)));
        final List<String> oneFeedOneLeft = CHUNK_FULL.subList(0, 2);

        // when
        final List<CompletableFuture<String>> incompleteBatch =
                new BatchBufferingService<>(BATCH_CAPACITY, BATCH_TIMEOUT_IN_SECONDS, oneSlotIsAvailable)
                        .prepareBatch(oneFeedOneLeft);

        assertThat(incompleteBatch).hasSize(BATCH_CAPACITY);
        assertThat(availableSlot.get()).isEqualTo(oneFeedOneLeft.get(0));
        assertThat(oneSlotIsAvailable).hasSize(BATCH_CAPACITY - 1); // one left, the rest are available
        assertThat(incompleteBatch.stream().filter(CompletableFuture::isDone).count()).isEqualTo(1);
        assertThat(incompleteBatch.stream().filter(CompletableFuture::isDone).findFirst().get().get()).isEqualTo(oneFeedOneLeft.get(1));
    }

    @Test
    public void shouldFeedLastIgnoringCompletedSlotAndReturnEmptyBatch() throws Exception {
        // given
        final CompletableFuture<String> completed = new CompletableFuture<>();
        completed.complete("already-completed");
        final CompletableFuture<String> availableSlot = new CompletableFuture<>();
        final LinkedBlockingQueue<CompletableFuture<String>> oneCompletedOneAvailable =
                spy(new LinkedBlockingQueue<>(List.of(completed, availableSlot)));
        final List<String> feedLast = CHUNK_FULL.subList(0, 1);

        // when
        final List<CompletableFuture<String>> emptyBatch =
                new BatchBufferingService<>(BATCH_CAPACITY, BATCH_TIMEOUT_IN_SECONDS, oneCompletedOneAvailable)
                        .prepareBatch(feedLast);

        assertThat(emptyBatch).isEmpty();
        assertThat(availableSlot.get()).isEqualTo(feedLast.get(0));
        assertThat(oneCompletedOneAvailable).isEmpty();
    }

    @Test
    public void shouldCompleteFreeSlotsInBatchToNullOnceTimeoutExceeded() throws Exception {
        // given
        final LinkedBlockingQueue<CompletableFuture<String>> noSlots = spy(new LinkedBlockingQueue<>());
        final List<String> keepOneAvailable = CHUNK_FULL.subList(0, 2);

        // when
        final List<CompletableFuture<String>> awaitingOneSlot =
                new BatchBufferingService<>(BATCH_CAPACITY, BATCH_TIMEOUT_IN_SECONDS, noSlots)
                        .prepareBatch(keepOneAvailable);

        assertThat(awaitingOneSlot).hasSize(BATCH_CAPACITY);
        // make sure it completes future anyway once timeout exceeded
        CompletableFuture.allOf(awaitingOneSlot.toArray(CompletableFuture[]::new)).get(BATCH_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
        assertThat(awaitingOneSlot.stream().map(future -> future.getNow(null)).filter(Objects::nonNull).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(keepOneAvailable);
        assertThat(awaitingOneSlot.stream().map(future -> future.getNow(null)).filter(Objects::isNull).count())
                .isEqualTo(1);
    }

    @Test
    public void shouldReturnEmptyBatchIfChunkIsEmpty() {
        // when
        final List<CompletableFuture<String>> awaitingOneSlot =
                new BatchBufferingService<String>(BATCH_CAPACITY, BATCH_TIMEOUT_IN_SECONDS, new LinkedBlockingQueue<>())
                        .prepareBatch(List.of());

        assertThat(awaitingOneSlot).isEmpty();
    }

    @Test
    public void shouldFailIfChunkIsLargerThanCapacity() {
        // when
        assertThatThrownBy(
                () -> new BatchBufferingService<String>(1, BATCH_TIMEOUT_IN_SECONDS, new LinkedBlockingQueue<>())
                        .prepareBatch(List.of("1", "2"))
        )
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Submitted chunk is larger than buffer capacity: 1");
    }
}