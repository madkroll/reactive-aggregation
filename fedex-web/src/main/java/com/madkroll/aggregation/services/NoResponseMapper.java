package com.madkroll.aggregation.services;

import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
public class NoResponseMapper {

    public <V> Map<String, V> noResponseMap(final Collection<String> keys) {
        final Map<String, V> result = new HashMap<>();
        keys.forEach(key -> result.put(key, null));
        return result;
    }
}
