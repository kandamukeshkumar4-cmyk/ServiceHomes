package com.servicehomes.api.search.domain;

public record SearchSuggestionProjection(
    String text,
    String sourceType
) {
}
