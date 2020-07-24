package com.madkroll.aggregation.services;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@RunWith(MockitoJUnitRunner.class)
public class DataProviderTest {

    private static final int TIMEOUT_IN_SECONDS = 10;
    private static final int BATCH_TIMEOUT_IN_SECONDS = 5;
    private static final int BATCH_CAPACITY = 5;

    private static final List<String> QUERIES =
            IntStream.range(0, 15)
                    .mapToObj(String::valueOf)
                    .map(next -> "query-" + next)
                    .collect(Collectors.toList());

    @Mock
    private DataProviderClient<String> client;

    @Mock
    private NoResponseMapper noResponseMapper;

    /*@Test
    public void shouldProcessBatchIfCapacityReached() {
        verifySingleBatchScenario(
                generateQueries(5),
                Duration.ofMillis(100),
                Duration.ofSeconds(10),
                false
        );

        verifyNoInteractions(noResponseMapper);
    }*/

    /*@Test
    public void shouldProcessBatchIfTimeHasCome() {
        verifySingleBatchScenario(
                generateQueries(2),
                Duration.ofMillis(100),
                Duration.ofSeconds(10),
                false
        );

        verifyNoInteractions(noResponseMapper);
    }*/

    /*@Test
    public void shouldReturnMapWithNullsIfErrorAppeared() {
        final List<String> queriesToEmit = generateQueries(5);
        verifySingleBatchScenario(
                queriesToEmit,
                Duration.ofMillis(100),
                Duration.ofSeconds(10),
                true
        );

        verify(noResponseMapper).noResponseMap(queriesToEmit);
    }

    private void verifySingleBatchScenario(
            final List<String> queriesToEmit,
            final Duration emissionDelay,
            final Duration expectResultsAfter,
            final boolean enforceFailure
    ) {
        final Map<String, String> backendResponse = queriesToEmit.stream().collect(toMap(Function.identity(), Function.identity()));

        final Map<String, String> finalExpectedResponse;
        if (enforceFailure) {
            given(client.retrieveByHttp(argThat(containsSameElementsAs(queriesToEmit)))).willThrow(IllegalStateException.class);

            final Map<String, String> noResponseMap = noResponseMap(backendResponse);
            given(noResponseMapper.<String>noResponseMap(argThat(containsSameElementsAs(queriesToEmit)))).willReturn(noResponseMap);
            finalExpectedResponse = noResponseMap;
        } else {
            given(client.retrieveByHttp(argThat(containsSameElementsAs(queriesToEmit)))).willReturn(Mono.just(backendResponse));
            finalExpectedResponse = backendResponse;
        }

        // then
        StepVerifier
                .withVirtualTime(
                        () -> {
                            final Flux<String> fluxProcessingQueue = Flux.fromIterable(queriesToEmit).delayElements(emissionDelay);

                            return new DataProvider<>(
                                    TIMEOUT_IN_SECONDS,
                                    BATCH_TIMEOUT_IN_SECONDS,
                                    BATCH_CAPACITY,
                                    client,
                                    new ConcurrentLinkedQueue<>(),
                                    fluxProcessingQueue,
                                    noResponseMapper
                            ).fetch(queriesToEmit);
                        }
                )
                .expectSubscription()
                .thenAwait(expectResultsAfter)
                .assertNext(
                        result -> assertThat(result).containsExactlyInAnyOrderEntriesOf(finalExpectedResponse)
                )
                .verifyComplete();

        verify(client).retrieveByHttp(argThat(containsSameElementsAs(queriesToEmit)));
    }*/

    private static ArgumentMatcher<Collection<String>> containsSameElementsAs(final Collection<String> firstBatch) {
        return argument -> argument.containsAll(firstBatch) && argument.size() == firstBatch.size();
    }

    private Map<String, String> noResponseMap(final Map<String, String> originalMap) {
        final Map<String, String> noResponseMap = new HashMap<>(originalMap);
        noResponseMap.replaceAll((key, oldValue) -> null);
        return noResponseMap;
    }

    private List<String> generateQueries(final int amount) {
        return IntStream.range(0, amount)
                .mapToObj(String::valueOf)
                .map(next -> "query-" + next)
                .collect(Collectors.toList());
    }
}