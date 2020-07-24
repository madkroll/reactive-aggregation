package com.madkroll.aggregation.services;

import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Log4j2
@AllArgsConstructor
public class BatchBufferingService<T> {

    // complete batch once it is full
    private final int batchCapacity;

    // complete batch in timeout if it's not full yet
    private final int batchTimeoutInSeconds;

    // this must be thread-safe queue only
    private final Queue<CompletableFuture<T>> availableComputationSlots;

    /**
     * Opens new immutable batch of completable futures per each item in the given chunk.
     * Batch has a fixed capacity.
     * <p>
     * There are such scenarios possible:
     * <ul>
     *     <li>if no chunks submitted - returns empty list</li>
     *     <li>if submitted chunk has more items than batch capacity, then throws IllegalArgumentException</li>
     *     <li>if number of items inside given chunk is equal to batch capacity, then builds new complete batch of them</li>
     *     <li>if submitted chunk has less items than batch capacity, then:
     *          <ul>
     *              <li>first it tries to feed items into other awaiting batches</li>
     *              <li>if no more items left, returns empty list</li>
     *              <li>if still any items left, builds new incomplete batch of them and publishes available slots,
     *              so other parallel calls are able to feed into these slots</li>
     *          </ul>
     *     </li>
     * </ul>
     *
     * Slots are open for fixed time threshold. Once awaiting time exceeded - completes slot with no value provided.
     *
     * @param chunk items to build batch from
     * @return batch of completable futures per each item from the given chunk
     */
    public List<CompletableFuture<T>> prepareBatch(final List<T> chunk) {
        if (CollectionUtils.isEmpty(chunk)) {
            log.debug("Chunk is empty. Ignore.");
            return ImmutableList.of();
        }

        if (chunk.size() > batchCapacity) {
            throw new IllegalArgumentException("Submitted chunk is larger than buffer capacity: " + batchCapacity);
        }

        if (chunk.size() == batchCapacity) {
            log.debug("Batch is complete. No buffering required.");
            return completeBatch(chunk);
        }

        final List<T> leftComputations = feed(chunk);
        if (leftComputations.isEmpty()) {
            log.debug("After feeding no more queries left. All will be processed by parallel batches.");
            return ImmutableList.of();
        }

        return ImmutableList.<CompletableFuture<T>>builder()
                .addAll(leftComputations.stream().map(CompletableFuture::completedFuture).collect(Collectors.toList()))
                .addAll(provideSlots(batchCapacity - leftComputations.size()))
                .build();
    }

    /**
     * Turns items from provided chunk into completed completable futures.
     * */
    private List<CompletableFuture<T>> completeBatch(final List<T> chunk) {
        return ImmutableList.copyOf(
                chunk.stream()
                        .map(CompletableFuture::completedFuture)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Attempts to feed as many items from the given chunk as possible to available slots of other awaiting batches.
     * Returns new chunk with left items.
     * */
    private List<T> feed(final List<T> chunk) {
        final List<T> leftItems = new ArrayList<>(chunk);

        final Iterator<T> chunkIterator = leftItems.iterator();
        while (chunkIterator.hasNext()) {
            if (feed(chunkIterator.next()).isEmpty()) {
                break;
            }

            // forget about this item, another batch takes care of it now
            chunkIterator.remove();
        }

        return leftItems;
    }

    /**
     * Feeds item into first available slot provided by any parallel incomplete batch.
     * Returns optional of future promising completed value once batch provided slot is processed.
     */
    private Optional<CompletableFuture<T>> feed(final T queryComputations) {
        CompletableFuture<T> freeSlot;
        while ((freeSlot = availableComputationSlots.poll()) != null) {
            log.debug("Queue: one slot is taken");
            if (freeSlot.complete(queryComputations)) {

                log.debug("Queue: feeding to slot has been successful");
                return Optional.of(freeSlot);
            }

            log.debug("Queue: taken slot is completed already");
        }

        log.debug("Queue: no slots available");
        return Optional.empty();
    }

    /**
     * Provides available slots that other incomplete batches can feed into.
     * If none of slots are taken withing time threshold closes slot by completing corresponding future with no value provided
     */
    private List<CompletableFuture<T>> provideSlots(final int availableSlotsNumber) {
        log.debug("Queue: provide available slots: {}", availableSlotsNumber);
        return IntStream.range(0, availableSlotsNumber)
                .mapToObj(next -> new CompletableFuture<T>())
                .map(slot -> slot.completeOnTimeout(null, batchTimeoutInSeconds, TimeUnit.SECONDS))
                .peek(availableComputationSlots::offer)
                .collect(Collectors.toList());
    }
}
