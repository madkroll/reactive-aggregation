package com.madkroll.aggregation.services;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
public class NoResponseMapper {

    /**
     * Produces map with null values set for all given keys.
     * */
    public <V> Map<String, V> noResponseMap(final Collection<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return Map.of();
        }

        final Map<String, V> result = new HashMap<>();
        keys.forEach(key -> result.put(key, null));
        return result;
    }
}
