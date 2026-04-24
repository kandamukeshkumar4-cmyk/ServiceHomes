package com.servicehomes.api.search.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SearchResponse(
    List<SearchResultItem> content,
    long totalElements,
    int totalPages,
    int currentPage,
    int pageSize,
    boolean hasNext,
    boolean hasPrevious,
    String cursor,
    java.util.UUID searchQueryId
) {
    public static SearchResponse of(
        List<SearchResultItem> content,
        long totalElements,
        int currentPage,
        int pageSize,
        java.util.UUID searchQueryId
    ) {
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        return new SearchResponse(
            content,
            totalElements,
            totalPages,
            currentPage,
            pageSize,
            currentPage < totalPages - 1,
            currentPage > 0,
            currentPage < totalPages - 1 ? encodeCursor(currentPage + 1, pageSize) : null,
            searchQueryId
        );
    }

    private static String encodeCursor(int page, int size) {
        return java.util.Base64.getUrlEncoder().encodeToString((page + ":" + size).getBytes());
    }
}
