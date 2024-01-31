package com.bislan.dynamicquery.library;

import java.util.Map;
import java.util.stream.Collectors;

public class QueryParameters {

    private final Map<String, String> parameters;

    public QueryParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return parameters.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
    }
}