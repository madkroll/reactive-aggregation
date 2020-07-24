package com.madkroll.aggregation.services;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class NoResponseMapperTest {

    @Test
    public void shouldReturnMapWithNullValuesOnly() {
        final Map<String, String> result = new NoResponseMapper().noResponseMap(List.of("1", "2"));

        final Map<String, String> expectedResult = new HashMap<>();
        expectedResult.put("1", null);
        expectedResult.put("2", null);
        assertThat(result).containsExactlyInAnyOrderEntriesOf(expectedResult);
    }

    @Test
    public void shouldReturnEmptyMapIfCollectionIsEmpty() {
        assertThat(new NoResponseMapper().noResponseMap(List.of()))
                .isEmpty();
    }

}